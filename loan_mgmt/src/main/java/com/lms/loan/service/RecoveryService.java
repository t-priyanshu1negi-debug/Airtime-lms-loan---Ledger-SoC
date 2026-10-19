package com.lms.loan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lms.loan.api.ApiException;
import com.lms.loan.api.dto.LoanDtos.AllocationView;
import com.lms.loan.api.dto.LoanDtos.ObligationView;
import com.lms.loan.api.dto.LoanDtos.QuoteRequest;
import com.lms.loan.api.dto.LoanDtos.QuoteView;
import com.lms.loan.api.dto.LoanDtos.ReleaseRequest;
import com.lms.loan.api.dto.LoanDtos.SettleRequest;
import com.lms.loan.api.dto.LoanDtos.SettlementView;
import com.lms.loan.api.dto.LoanDtos.SignalView;
import com.lms.loan.api.dto.LoanDtos.VoluntaryPaymentRequest;
import com.lms.loan.domain.AdvanceStatus;
import com.lms.loan.domain.AllocationComponent;
import com.lms.loan.domain.AllocationEngine;
import com.lms.loan.domain.Ids;
import com.lms.loan.domain.IntentStatus;
import com.lms.loan.domain.MandateStatus;
import com.lms.loan.domain.Money;
import com.lms.loan.domain.PostingSource;
import com.lms.loan.domain.QuoteCalculator;
import com.lms.loan.domain.ReservationVerb;
import com.lms.loan.domain.Utc;
import com.lms.loan.domain.entity.AdvanceAgreement;
import com.lms.loan.domain.entity.Allocation;
import com.lms.loan.domain.entity.AllocationKey;
import com.lms.loan.domain.entity.AttemptKey;
import com.lms.loan.domain.entity.CollectionReservation;
import com.lms.loan.domain.entity.DeductionIntent;
import com.lms.loan.domain.entity.Mandate;
import com.lms.loan.domain.entity.PaymentPosting;
import com.lms.loan.domain.entity.PostingKey;
import com.lms.loan.domain.entity.RecoveryAttempt;
import com.lms.loan.domain.entity.ReservationKey;
import com.lms.loan.repo.AdvanceRepository;
import com.lms.loan.repo.AllocationRepository;
import com.lms.loan.repo.DeductionIntentRepository;
import com.lms.loan.repo.DisputeRepository;
import com.lms.loan.repo.MandateRepository;
import com.lms.loan.repo.PaymentPostingRepository;
import com.lms.loan.repo.RecoveryAttemptRepository;
import com.lms.loan.repo.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RecoveryService {

    private static final List<String> COLLECTABLE = List.of(
            AdvanceStatus.DISBURSED.name(),
            AdvanceStatus.PARTIALLY_RECOVERED.name(),
            AdvanceStatus.EXPIRED.name(),
            AdvanceStatus.WRITTEN_OFF.name());

    private final AdvanceRepository advanceRepository;
    private final MandateRepository mandateRepository;
    private final DisputeRepository disputeRepository;
    private final DeductionIntentRepository intentRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentPostingRepository postingRepository;
    private final AllocationRepository allocationRepository;
    private final RecoveryAttemptRepository attemptRepository;
    private final OutboxWriter outboxWriter;
    private final LoanViews views;
    private final ObjectMapper objectMapper;

    public RecoveryService(AdvanceRepository advanceRepository,
                           MandateRepository mandateRepository,
                           DisputeRepository disputeRepository,
                           DeductionIntentRepository intentRepository,
                           ReservationRepository reservationRepository,
                           PaymentPostingRepository postingRepository,
                           AllocationRepository allocationRepository,
                           RecoveryAttemptRepository attemptRepository,
                           OutboxWriter outboxWriter,
                           LoanViews views,
                           ObjectMapper objectMapper) {
        this.advanceRepository = advanceRepository;
        this.mandateRepository = mandateRepository;
        this.disputeRepository = disputeRepository;
        this.intentRepository = intentRepository;
        this.reservationRepository = reservationRepository;
        this.postingRepository = postingRepository;
        this.allocationRepository = allocationRepository;
        this.attemptRepository = attemptRepository;
        this.outboxWriter = outboxWriter;
        this.views = views;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public QuoteView quote(QuoteRequest request) {
        String signalId = request.signalId().trim();
        DeductionIntent existing = intentRepository.findBySignalId(signalId).orElse(null);
        if (existing != null) {
            return quoteView(existing, obligations(existing.getSubscriberRef()));
        }
        String subscriber = request.subscriberRef().trim();
        String currency = request.currency().trim().toUpperCase();
        List<AdvanceAgreement> live = advanceRepository.lockBySubscriberAndStatus(subscriber, COLLECTABLE);
        assertCurrency(live, currency);
        BigDecimal collectable = collectableOf(live);
        List<Mandate> mandates = mandateRepository.findBySubscriberRefAndStatus(subscriber, MandateStatus.ACTIVE.name());
        String refusal = refusal(request, subscriber, mandates);
        BigDecimal quoted = Money.zero();
        BigDecimal mandateShare = narrowest(mandates);
        if (refusal == null) {
            QuoteCalculator.Result result = QuoteCalculator.quote(new QuoteCalculator.Input(
                    Money.of(request.grossRecharge()),
                    request.maxShare(),
                    mandateShare,
                    collectable,
                    request.crossProductHeadroom() == null ? null : Money.of(request.crossProductHeadroom()),
                    request.neverStrandFloor() == null ? null : Money.of(request.neverStrandFloor())));
            quoted = result.quoted();
            refusal = result.refusalCode();
        }
        DeductionIntent intent = new DeductionIntent();
        intent.setIntentId(Ids.uuidV7());
        intent.setSignalId(signalId);
        intent.setSubscriberRef(subscriber);
        intent.setGrossRecharge(Money.of(request.grossRecharge()));
        intent.setQuotedAmount(quoted);
        intent.setStatus(refusal == null ? IntentStatus.QUOTED.name() : IntentStatus.REFUSED.name());
        intent.setCreatedAt(Utc.now());
        intentRepository.save(intent);
        UUID reservationId = null;
        if (refusal == null) {
            reservationId = reserve(signalId, subscriber, quoted, currency, null, ReservationVerb.RESERVE);
        }
        return new QuoteView(intent.getIntentId(), signalId, subscriber, intent.getStatus(),
                intent.getGrossRecharge(), collectable, quoted, refusal, reservationId, obligationViews(live));
    }

    @Transactional
    public SettlementView settle(SettleRequest request, String idempotencyHeader) {
        DeductionIntent intent = intentRepository.findBySignalId(request.signalId().trim())
                .orElseThrow(() -> ApiException.notFound("SIGNAL_NOT_FOUND", "No deduction intent for that signal"));
        if (IntentStatus.REFUSED.name().equals(intent.getStatus())) {
            throw ApiException.conflict("SIGNAL_REFUSED", "A refused quote is not settled");
        }
        String key = idempotencyKey(idempotencyHeader, request.idempotencyKey(), request.signalId().trim() + ":SETTLE");
        PaymentPosting existing = findPosting(key);
        if (existing != null) {
            return settlementView(intent, existing);
        }
        if (IntentStatus.SETTLED.name().equals(intent.getStatus())) {
            throw ApiException.conflict("ALREADY_SETTLED", "Signal is already settled under a different idempotency key");
        }
        String currency = request.currency().trim().toUpperCase();
        List<AdvanceAgreement> live = advanceRepository.lockBySubscriberAndStatus(intent.getSubscriberRef(), COLLECTABLE);
        assertCurrency(live, currency);
        LocalDateTime settledAt = request.settledAt() == null
                ? Utc.now()
                : request.settledAt().truncatedTo(ChronoUnit.MICROS);
        PaymentPosting posting = new PaymentPosting();
        posting.setId(new PostingKey(Ids.uuidV7(), settledAt));
        posting.setIdempotencyKey(key);
        posting.setSubscriberRef(intent.getSubscriberRef());
        posting.setSource(PostingSource.RECHARGE_SWEEP.name());
        posting.setSignalId(intent.getSignalId());
        posting.setAmount(Money.of(request.settledAmount()));
        posting.setCurrency(currency);
        posting.setAuthorised(request.authorised());
        postingRepository.save(posting);

        List<AllocationView> allocations = applyAllocation(live, posting);
        if (hasVerb(intent.getSignalId(), ReservationVerb.RESERVE)
                && !hasVerb(intent.getSignalId(), ReservationVerb.CONSUME)
                && !hasVerb(intent.getSignalId(), ReservationVerb.RELEASE)) {
            reserve(intent.getSignalId(), intent.getSubscriberRef(), posting.getAmount(), currency, null, ReservationVerb.CONSUME);
        }
        attempt(intent, "SETTLED", conduct(intent, live, posting, request.authorised()));
        intent.setStatus(IntentStatus.SETTLED.name());
        intentRepository.save(intent);
        return settlementView(intent, posting, allocations);
    }

    @Transactional
    public QuoteView release(ReleaseRequest request) {
        DeductionIntent intent = intentRepository.findBySignalId(request.signalId().trim())
                .orElseThrow(() -> ApiException.notFound("SIGNAL_NOT_FOUND", "No deduction intent for that signal"));
        if (IntentStatus.SETTLED.name().equals(intent.getStatus())) {
            throw ApiException.conflict("ALREADY_SETTLED", "A settled signal cannot be released");
        }
        if (IntentStatus.REFUSED.name().equals(intent.getStatus())) {
            throw ApiException.conflict("SIGNAL_REFUSED", "A refused quote has no reservation to release");
        }
        if (IntentStatus.QUOTED.name().equals(intent.getStatus())) {
            String currency = reservationRepository.findBySignalIdOrderByIdCreatedAtAsc(intent.getSignalId()).stream()
                    .map(CollectionReservation::getCurrency)
                    .findFirst()
                    .orElse("XXX");
            reserve(intent.getSignalId(), intent.getSubscriberRef(), intent.getQuotedAmount(), currency,
                    request.refusalCode().trim(), ReservationVerb.RELEASE);
            intent.setStatus(IntentStatus.RELEASED.name());
            intentRepository.save(intent);
        }
        return quoteView(intent, obligations(intent.getSubscriberRef()));
    }

    @Transactional
    public SettlementView voluntary(VoluntaryPaymentRequest request, String idempotencyHeader) {
        String key = idempotencyKey(idempotencyHeader, request.idempotencyKey(), null);
        PaymentPosting existing = findPosting(key);
        if (existing != null) {
            if (!existing.getSubscriberRef().equals(request.subscriberRef().trim())
                    || existing.getAmount().compareTo(Money.of(request.amount())) != 0) {
                throw ApiException.conflict("IDEMPOTENCY_CONFLICT", "Idempotency key was already used for a different payment");
            }
            return settlementView(null, existing);
        }
        String currency = request.currency().trim().toUpperCase();
        List<AdvanceAgreement> live = advanceRepository.lockBySubscriberAndStatus(request.subscriberRef().trim(), COLLECTABLE);
        assertCurrency(live, currency);
        LocalDateTime settledAt = Utc.now();
        PaymentPosting posting = new PaymentPosting();
        posting.setId(new PostingKey(Ids.uuidV7(), settledAt));
        posting.setIdempotencyKey(key);
        posting.setSubscriberRef(request.subscriberRef().trim());
        posting.setSource(PostingSource.VOLUNTARY.name());
        posting.setAmount(Money.of(request.amount()));
        posting.setCurrency(currency);
        posting.setAuthorised(true);
        postingRepository.save(posting);
        List<AllocationView> allocations = applyAllocation(live, posting);
        return settlementView(null, posting, allocations);
    }

    @Transactional(readOnly = true)
    public SignalView signal(String signalId) {
        DeductionIntent intent = intentRepository.findBySignalId(signalId)
                .orElseThrow(() -> ApiException.notFound("SIGNAL_NOT_FOUND", "No deduction intent for that signal"));
        return new SignalView(
                quoteView(intent, obligations(intent.getSubscriberRef())),
                reservationRepository.findBySignalIdOrderByIdCreatedAtAsc(signalId).stream().map(views::reservation).toList(),
                attemptRepository.findByIntentId(intent.getIntentId()).stream().map(views::attempt).toList());
    }

    private String refusal(QuoteRequest request, String subscriber, List<Mandate> mandates) {
        if (!Boolean.TRUE.equals(request.sweepable())) {
            return "NOT_SWEEPABLE";
        }
        if (mandates.isEmpty()) {
            return "MANDATE_NOT_ACTIVE";
        }
        List<UUID> ids = advanceRepository.findBySubscriberRefOrderByDueDateAscDrawnAtAscAdvanceIdAsc(subscriber).stream()
                .map(AdvanceAgreement::getAdvanceId)
                .toList();
        if (!ids.isEmpty() && disputeRepository.existsByAdvanceIdInAndHoldActiveTrue(ids)) {
            return "DISPUTE_HOLD";
        }
        return null;
    }

    private List<AllocationView> applyAllocation(List<AdvanceAgreement> live, PaymentPosting posting) {
        Map<UUID, AdvanceAgreement> byId = new HashMap<>();
        List<AllocationEngine.Obligation> obligations = new ArrayList<>();
        for (AdvanceAgreement advance : live) {
            byId.put(advance.getAdvanceId(), advance);
            obligations.add(new AllocationEngine.Obligation(
                    advance.getAdvanceId(), advance.getDueDate(), advance.getDrawnAt(),
                    advance.getFeeResidual(), advance.getPrincipalResidual()));
        }
        List<AllocationEngine.Slice> slices = AllocationEngine.allocate(obligations, posting.getAmount());
        Map<UUID, BigDecimal> recoveredOnWrittenOff = new HashMap<>();
        java.util.Set<UUID> touched = new java.util.LinkedHashSet<>();
        List<AllocationView> viewsOut = new ArrayList<>();
        for (AllocationEngine.Slice slice : slices) {
            touched.add(slice.advanceId());
            AdvanceAgreement advance = byId.get(slice.advanceId());
            if (slice.component() == AllocationComponent.FEE) {
                advance.setFeeResidual(Money.of(advance.getFeeResidual()).subtract(slice.amount()));
            } else {
                advance.setPrincipalResidual(Money.of(advance.getPrincipalResidual()).subtract(slice.amount()));
            }
            Allocation row = new Allocation();
            row.setId(new AllocationKey(Ids.uuidV7(), posting.getId().getSettledAt()));
            row.setPostingId(posting.getId().getPostingId());
            row.setAdvanceId(slice.advanceId());
            row.setComponent(slice.component().name());
            row.setAmount(slice.amount());
            row.setSeq((short) slice.seq());
            allocationRepository.save(row);
            viewsOut.add(views.allocation(row));
            if (AdvanceStatus.WRITTEN_OFF.name().equals(advance.getStatus())) {
                recoveredOnWrittenOff.merge(advance.getAdvanceId(), slice.amount(), BigDecimal::add);
            }
        }
        for (UUID touchedId : touched) {
            AdvanceAgreement advance = byId.get(touchedId);
            BigDecimal residual = Money.of(advance.getFeeResidual()).add(Money.of(advance.getPrincipalResidual()));
            boolean writtenOff = AdvanceStatus.WRITTEN_OFF.name().equals(advance.getStatus());
            if (residual.signum() == 0 && !writtenOff) {
                advance.setStatus(AdvanceStatus.RECOVERED.name());
                outboxWriter.loan(advance.getAdvanceId(), "ADVANCE_RECOVERED",
                        Map.of("advanceId", advance.getAdvanceId().toString()));
            } else if (residual.signum() > 0 && !writtenOff
                    && !AdvanceStatus.PARTIALLY_RECOVERED.name().equals(advance.getStatus())) {
                advance.setStatus(AdvanceStatus.PARTIALLY_RECOVERED.name());
            }
            advanceRepository.save(advance);
        }
        recoveredOnWrittenOff.forEach((advanceId, amount) -> {
            AdvanceAgreement advance = byId.get(advanceId);
            outboxWriter.accounting(advanceId, "RECOVERY_OF_WRITTEN_OFF", AccountingFact.recoveryOfWrittenOff(
                    advanceId, advance.getMarketCode(), advance.getCurrency(), amount));
        });
        return viewsOut;
    }

    private UUID reserve(String signalId, String subscriber, BigDecimal amount, String currency,
                         String refusalCode, ReservationVerb verb) {
        UUID id = Ids.uuidV7();
        CollectionReservation row = new CollectionReservation();
        row.setId(new ReservationKey(id, Utc.now()));
        row.setSignalId(signalId);
        row.setSubscriberRef(subscriber);
        row.setVerb(verb.name());
        row.setAmount(Money.of(amount));
        row.setCurrency(currency);
        row.setRefusalCode(refusalCode);
        reservationRepository.save(row);
        return id;
    }

    private void attempt(DeductionIntent intent, String outcome, String conduct) {
        RecoveryAttempt row = new RecoveryAttempt();
        row.setId(new AttemptKey(Ids.uuidV7(), Utc.now()));
        row.setIntentId(intent.getIntentId());
        row.setOutcome(outcome);
        row.setConductRecord(conduct);
        attemptRepository.save(row);
    }

    private String conduct(DeductionIntent intent, List<AdvanceAgreement> live, PaymentPosting posting, boolean authorised) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("mandate_status", MandateStatus.ACTIVE.name());
        node.put("dispute_hold", false);
        node.put("gross", Money.of(intent.getGrossRecharge()));
        node.put("collectable", collectableOf(live));
        node.put("quoted", Money.of(intent.getQuotedAmount()));
        node.put("settled", Money.of(posting.getAmount()));
        node.put("source", posting.getSource());
        node.put("authorised", authorised);
        return node.toString();
    }

    private QuoteView quoteView(DeductionIntent intent, List<ObligationView> obligations) {
        UUID reservationId = reservationRepository.findBySignalIdOrderByIdCreatedAtAsc(intent.getSignalId()).stream()
                .filter(row -> ReservationVerb.RESERVE.name().equals(row.getVerb()))
                .map(row -> row.getId().getReservationId())
                .findFirst()
                .orElse(null);
        String refusal = IntentStatus.QUOTED.name().equals(intent.getStatus()) || IntentStatus.SETTLED.name().equals(intent.getStatus())
                || IntentStatus.RELEASED.name().equals(intent.getStatus())
                ? null
                : "REFUSED";
        BigDecimal collectable = obligations.stream()
                .map(row -> row.feeResidual().add(row.principalResidual()))
                .reduce(Money.zero(), BigDecimal::add);
        return new QuoteView(intent.getIntentId(), intent.getSignalId(), intent.getSubscriberRef(), intent.getStatus(),
                Money.of(intent.getGrossRecharge()), collectable, Money.of(intent.getQuotedAmount()),
                refusal, reservationId, obligations);
    }

    private SettlementView settlementView(DeductionIntent intent, PaymentPosting posting) {
        List<AllocationView> allocations = allocationRepository.findByPostingIdOrderBySeqAsc(posting.getId().getPostingId())
                .stream().map(views::allocation).toList();
        return settlementView(intent, posting, allocations);
    }

    private SettlementView settlementView(DeductionIntent intent, PaymentPosting posting, List<AllocationView> allocations) {
        BigDecimal allocated = allocations.stream().map(AllocationView::amount).reduce(Money.zero(), BigDecimal::add);
        BigDecimal settled = Money.of(posting.getAmount());
        return new SettlementView(
                intent == null ? null : intent.getIntentId(),
                posting.getSignalId(),
                intent == null ? IntentStatus.SETTLED.name() : intent.getStatus(),
                posting.getId().getPostingId(),
                posting.getId().getSettledAt(),
                settled,
                allocated,
                settled.subtract(allocated),
                Boolean.TRUE.equals(posting.getAuthorised()),
                allocations);
    }

    private List<ObligationView> obligations(String subscriber) {
        return advanceRepository.findBySubscriberRefOrderByDueDateAscDrawnAtAscAdvanceIdAsc(subscriber).stream()
                .filter(row -> AdvanceStatus.valueOf(row.getStatus()).collectable())
                .map(this::obligation)
                .toList();
    }

    private List<ObligationView> obligationViews(List<AdvanceAgreement> live) {
        return live.stream()
                .sorted(Comparator.comparing(AdvanceAgreement::getDueDate)
                        .thenComparing(AdvanceAgreement::getDrawnAt)
                        .thenComparing(AdvanceAgreement::getAdvanceId))
                .map(this::obligation)
                .toList();
    }

    private ObligationView obligation(AdvanceAgreement advance) {
        return new ObligationView(advance.getAdvanceId(), advance.getDueDate(),
                Money.of(advance.getFeeResidual()), Money.of(advance.getPrincipalResidual()), advance.getStatus());
    }

    private static BigDecimal collectableOf(List<AdvanceAgreement> live) {
        return live.stream()
                .map(row -> Money.of(row.getFeeResidual()).add(Money.of(row.getPrincipalResidual())))
                .reduce(Money.zero(), BigDecimal::add);
    }

    private static BigDecimal narrowest(List<Mandate> mandates) {
        return mandates.stream()
                .map(Mandate::getMandateShare)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
    }

    private static void assertCurrency(List<AdvanceAgreement> live, String currency) {
        boolean mismatch = live.stream().map(AdvanceAgreement::getCurrency).anyMatch(value -> !value.equals(currency));
        long distinct = live.stream().map(AdvanceAgreement::getCurrency).distinct().count();
        if (mismatch || distinct > 1) {
            throw ApiException.conflict("CURRENCY_MISMATCH", "Quote currency must match every live advance");
        }
    }

    private boolean hasVerb(String signalId, ReservationVerb verb) {
        return reservationRepository.findBySignalIdOrderByIdCreatedAtAsc(signalId).stream()
                .anyMatch(row -> verb.name().equals(row.getVerb()));
    }

    private PaymentPosting findPosting(String idempotencyKey) {
        List<PaymentPosting> rows = postingRepository.findByIdempotencyKey(idempotencyKey);
        if (rows.isEmpty()) {
            return null;
        }
        return rows.get(0);
    }

    private static String idempotencyKey(String header, String body, String fallback) {
        String headerKey = blankToNull(header);
        String bodyKey = blankToNull(body);
        if (headerKey != null && bodyKey != null && !headerKey.equals(bodyKey)) {
            throw ApiException.conflict("IDEMPOTENCY_CONFLICT", "Idempotency-Key header and body key differ");
        }
        String key = headerKey != null ? headerKey : bodyKey;
        if (key == null) {
            key = fallback;
        }
        if (key == null || key.isBlank()) {
            throw ApiException.badRequest("IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key is required");
        }
        return key;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
