package com.lms.loan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.loan.api.ApiException;
import com.lms.loan.api.Enums;
import com.lms.loan.api.WriteResult;
import com.lms.loan.api.dto.LoanDtos.AcknowledgeRequest;
import com.lms.loan.api.dto.LoanDtos.AdvanceView;
import com.lms.loan.api.dto.LoanDtos.CancelRequest;
import com.lms.loan.api.dto.LoanDtos.CreateAdvanceRequest;
import com.lms.loan.api.dto.LoanDtos.CreateDisclosureRequest;
import com.lms.loan.api.dto.LoanDtos.CreateMandateRequest;
import com.lms.loan.api.dto.LoanDtos.CreditResultRequest;
import com.lms.loan.api.dto.LoanDtos.DisclosureView;
import com.lms.loan.api.dto.LoanDtos.MandateView;
import com.lms.loan.domain.AdvanceStatus;
import com.lms.loan.domain.FeeMethod;
import com.lms.loan.domain.Ids;
import com.lms.loan.domain.MandateStatus;
import com.lms.loan.domain.Money;
import com.lms.loan.domain.ProductType;
import com.lms.loan.domain.ScheduleEventType;
import com.lms.loan.domain.Utc;
import com.lms.loan.domain.entity.AdvanceAgreement;
import com.lms.loan.domain.entity.Disclosure;
import com.lms.loan.domain.entity.DunningState;
import com.lms.loan.domain.entity.Mandate;
import com.lms.loan.domain.entity.ScheduleEvent;
import com.lms.loan.repo.AdvanceRepository;
import com.lms.loan.repo.AllocationRepository;
import com.lms.loan.repo.DisclosureRepository;
import com.lms.loan.repo.DunningStateRepository;
import com.lms.loan.repo.MandateRepository;
import com.lms.loan.repo.ScheduleEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

@Service
public class OriginationService {

    private static final int DISCLOSURE_BUDGET = 182;

    private final DisclosureRepository disclosureRepository;
    private final MandateRepository mandateRepository;
    private final AdvanceRepository advanceRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final DunningStateRepository dunningStateRepository;
    private final AllocationRepository allocationRepository;
    private final OutboxWriter outboxWriter;
    private final LoanViews views;
    private final ObjectMapper objectMapper;

    public OriginationService(DisclosureRepository disclosureRepository,
                              MandateRepository mandateRepository,
                              AdvanceRepository advanceRepository,
                              ScheduleEventRepository scheduleEventRepository,
                              DunningStateRepository dunningStateRepository,
                              AllocationRepository allocationRepository,
                              OutboxWriter outboxWriter,
                              LoanViews views,
                              ObjectMapper objectMapper) {
        this.disclosureRepository = disclosureRepository;
        this.mandateRepository = mandateRepository;
        this.advanceRepository = advanceRepository;
        this.scheduleEventRepository = scheduleEventRepository;
        this.dunningStateRepository = dunningStateRepository;
        this.allocationRepository = allocationRepository;
        this.outboxWriter = outboxWriter;
        this.views = views;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WriteResult<DisclosureView> createDisclosure(CreateDisclosureRequest request) {
        if (Boolean.TRUE.equals(request.truncated()) || request.renderedText().length() > DISCLOSURE_BUDGET) {
            throw ApiException.unprocessable("DISCLOSURE_UNRENDERABLE",
                    "Disclosure cash core must fit in 182 characters. Truncation is a refusal.");
        }
        BigDecimal expected = Money.of(request.amount()).add(Money.of(request.fee()));
        if (expected.compareTo(Money.of(request.totalRepayable())) != 0) {
            throw ApiException.badRequest("TOTAL_MISMATCH", "totalRepayable must equal amount + fee");
        }
        return disclosureRepository.findByAckToken(request.ackToken())
                .map(existing -> new WriteResult<>(views.disclosure(existing), false))
                .orElseGet(() -> {
                    Disclosure row = new Disclosure();
                    row.setDisclosureId(Ids.uuidV7());
                    row.setSubscriberRef(request.subscriberRef().trim());
                    row.setPidVersion(request.pidVersion());
                    row.setAmount(Money.of(request.amount()));
                    row.setFee(Money.of(request.fee()));
                    row.setTotalRepayable(Money.of(request.totalRepayable()));
                    row.setRenderedText(request.renderedText());
                    row.setAckToken(request.ackToken().trim());
                    return new WriteResult<>(views.disclosure(disclosureRepository.save(row)), true);
                });
    }

    @Transactional
    public DisclosureView acknowledge(AcknowledgeRequest request) {
        Disclosure row = disclosureRepository.findByAckToken(request.ackToken().trim())
                .orElseThrow(() -> ApiException.notFound("DISCLOSURE_NOT_FOUND", "No disclosure for that acknowledgement token"));
        if (row.getAcknowledgedAt() == null) {
            row.setAcknowledgedAt(Utc.now());
            disclosureRepository.save(row);
        }
        return views.disclosure(row);
    }

    @Transactional(readOnly = true)
    public DisclosureView getDisclosure(UUID disclosureId) {
        return views.disclosure(requireDisclosure(disclosureId));
    }

    @Transactional
    public MandateView armMandate(CreateMandateRequest request) {
        Disclosure disclosure = requireDisclosure(request.disclosureId());
        if (!disclosure.getSubscriberRef().equals(request.subscriberRef().trim())) {
            throw ApiException.conflict("SUBSCRIBER_MISMATCH", "Mandate subscriber does not match the disclosure");
        }
        if (disclosure.getAcknowledgedAt() == null) {
            throw ApiException.unprocessable("DISCLOSURE_NOT_ACKNOWLEDGED",
                    "Mandate is committed only after the disclosure is acknowledged");
        }
        Mandate row = new Mandate();
        row.setMandateId(Ids.uuidV7());
        row.setSubscriberRef(request.subscriberRef().trim());
        row.setDisclosureId(disclosure.getDisclosureId());
        row.setMandateShare(request.mandateShare().setScale(6));
        row.setStatus(MandateStatus.ACTIVE.name());
        row.setArmedAt(Utc.now());
        mandateRepository.save(row);
        outboxWriter.append(OutboxWriter.MANDATE_EVENTS, row.getSubscriberRef(), "MANDATE_ARMED",
                Map.of("mandateId", row.getMandateId().toString(), "subscriberRef", row.getSubscriberRef(),
                        "mandateShare", row.getMandateShare()));
        return views.mandate(row);
    }

    @Transactional
    public MandateView revoke(UUID mandateId) {
        Mandate row = mandateRepository.findById(mandateId)
                .orElseThrow(() -> ApiException.notFound("MANDATE_NOT_FOUND", "Mandate not found"));
        if (!MandateStatus.REVOKED.name().equals(row.getStatus())) {
            row.setStatus(MandateStatus.REVOKED.name());
            row.setRevokedAt(Utc.now());
            mandateRepository.save(row);
            outboxWriter.append(OutboxWriter.MANDATE_EVENTS, row.getSubscriberRef(), "MANDATE_REVOKED",
                    Map.of("mandateId", row.getMandateId().toString(), "subscriberRef", row.getSubscriberRef()));
        }
        return views.mandate(row);
    }

    @Transactional
    public WriteResult<AdvanceView> createAdvance(CreateAdvanceRequest request) {
        return advanceRepository.findByOriginationKey(request.originationKey().trim())
                .map(existing -> new WriteResult<>(views.advance(existing), false))
                .orElseGet(() -> new WriteResult<>(views.advance(insertAdvance(request)), true));
    }

    private AdvanceAgreement insertAdvance(CreateAdvanceRequest request) {
        Disclosure disclosure = requireDisclosure(request.disclosureId());
        String subscriber = request.subscriberRef().trim();
        if (!disclosure.getSubscriberRef().equals(subscriber)) {
            throw ApiException.conflict("SUBSCRIBER_MISMATCH", "Advance subscriber does not match the disclosure");
        }
        if (disclosure.getAcknowledgedAt() == null) {
            throw ApiException.unprocessable("DISCLOSURE_NOT_ACKNOWLEDGED", "Create-advance requires an acknowledged disclosure");
        }
        if (disclosure.getAdvanceId() != null) {
            throw ApiException.conflict("DISCLOSURE_ALREADY_LINKED", "This disclosure is already linked to an advance");
        }
        if (disclosure.getPidVersion().intValue() != request.pidVersion()
                || disclosure.getAmount().compareTo(Money.of(request.principalAmount())) != 0
                || disclosure.getFee().compareTo(Money.of(request.feeAmount())) != 0) {
            throw ApiException.conflict("PRICING_CHANGED", "PID version, principal, or fee changed after the disclosure was rendered");
        }
        Mandate mandate = resolveMandate(request.mandateId(), subscriber);
        if (!MandateStatus.ACTIVE.name().equals(mandate.getStatus())) {
            throw ApiException.unprocessable("MANDATE_NOT_ACTIVE", "An active mandate is required before create-advance");
        }
        ProductType productType = Enums.parse(ProductType.class, request.productType(), "PRODUCT_TYPE_INVALID");
        FeeMethod feeMethod = Enums.parse(FeeMethod.class, request.feeMethod(), "FEE_METHOD_INVALID");

        LocalDateTime drawnAt = request.drawnAt() == null
                ? Utc.now()
                : request.drawnAt().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        AdvanceAgreement advance = new AdvanceAgreement();
        advance.setAdvanceId(Ids.uuidV7());
        advance.setMarketCode(request.marketCode().trim());
        advance.setSubscriberRef(subscriber);
        advance.setMsisdnHash(Enums.msisdnHash(request.msisdnHashHex()));
        advance.setMsisdnValidFrom(request.msisdnValidFrom());
        advance.setOriginationKey(request.originationKey().trim());
        advance.setProductType(productType.name());
        advance.setSkuId(request.skuId().trim());
        advance.setPidId(request.pidId().trim());
        advance.setPidVersion(request.pidVersion());
        advance.setCurrency(request.currency().trim().toUpperCase());
        advance.setLenderOfRecord(request.lenderOfRecord().trim());
        advance.setRegulatedFlag(request.regulatedFlag());
        advance.setPrincipalAmount(Money.of(request.principalAmount()));
        advance.setFeeAmount(Money.of(request.feeAmount()));
        advance.setFeeMethod(feeMethod.name());
        advance.setPrincipalResidual(Money.of(request.principalAmount()));
        advance.setFeeResidual(Money.of(request.feeAmount()));
        advance.setStatus(AdvanceStatus.PENDING.name());
        advance.setDrawnAt(drawnAt);
        advance.setDueDate(request.dueDate());
        advance.setGraceDatetime(request.dueDate().plusDays(request.graceDays()).atTime(LocalTime.of(23, 59, 59)));
        advanceRepository.save(advance);

        disclosure.setAdvanceId(advance.getAdvanceId());
        disclosureRepository.save(disclosure);
        schedule(advance.getAdvanceId(), ScheduleEventType.SCHEDULED, Map.of(
                "dueDate", advance.getDueDate().toString(),
                "graceDays", request.graceDays()));
        outboxWriter.loan(advance.getAdvanceId(), "ADVANCE_CREATED", Map.of(
                "advanceId", advance.getAdvanceId().toString(),
                "status", advance.getStatus(),
                "originationKey", advance.getOriginationKey()));
        return advance;
    }

    @Transactional
    public AdvanceView creditResult(UUID advanceId, CreditResultRequest request) {
        AdvanceAgreement advance = requireAdvance(advanceId);
        String outcome = request.outcome().trim().toUpperCase();
        AdvanceStatus status = AdvanceStatus.valueOf(advance.getStatus());
        if ("SUCCESS".equals(outcome)) {
            if (status == AdvanceStatus.DISBURSED) {
                return views.advance(advance);
            }
            if (status != AdvanceStatus.PENDING) {
                throw ApiException.conflict("ILLEGAL_STATE", "Credit success is only valid from PENDING");
            }
            advance.setStatus(AdvanceStatus.DISBURSED.name());
            advanceRepository.save(advance);
            DunningState dunning = new DunningState();
            dunning.setAdvanceId(advance.getAdvanceId());
            dunning.setDaysPastDue(0);
            dunning.setAgingBucket(0);
            dunning.setLadderStep(0);
            dunning.setUpdatedAt(Utc.now());
            dunningStateRepository.save(dunning);
            outboxWriter.loan(advance.getAdvanceId(), "ADVANCE_DISBURSED", Map.of("advanceId", advance.getAdvanceId().toString()));
            if (Money.positive(advance.getFeeAmount())) {
                outboxWriter.accounting(advance.getAdvanceId(), "FEE_BOOKED", AccountingFact.feeRevenue(
                        advance.getAdvanceId(), advance.getMarketCode(), advance.getCurrency(), advance.getFeeAmount()));
            }
            return views.advance(advance);
        }
        if ("EXHAUSTED".equals(outcome)) {
            if (status == AdvanceStatus.ABORTED) {
                return views.advance(advance);
            }
            if (status != AdvanceStatus.PENDING) {
                throw ApiException.conflict("ILLEGAL_STATE", "Credit exhaustion is only valid from PENDING");
            }
            advance.setStatus(AdvanceStatus.ABORTED.name());
            advance.setPrincipalResidual(Money.zero());
            advance.setFeeResidual(Money.zero());
            advanceRepository.save(advance);
            outboxWriter.loan(advance.getAdvanceId(), "ADVANCE_ABORTED", Map.of("advanceId", advance.getAdvanceId().toString()));
            return views.advance(advance);
        }
        throw ApiException.badRequest("CREDIT_RESULT_INVALID", "outcome must be SUCCESS or EXHAUSTED");
    }

    @Transactional
    public AdvanceView cancel(UUID advanceId, CancelRequest request) {
        AdvanceAgreement advance = requireAdvance(advanceId);
        AdvanceStatus status = AdvanceStatus.valueOf(advance.getStatus());
        if (status != AdvanceStatus.PENDING && status != AdvanceStatus.DISBURSED) {
            throw ApiException.conflict("NOT_CANCELLABLE", "Cooling-off cancellation applies to PENDING or DISBURSED advances");
        }
        if (allocationRepository.existsByAdvanceId(advanceId)) {
            throw ApiException.conflict("NOT_CANCELLABLE", "An advance with posted allocation cannot be cancelled");
        }
        LocalDateTime deadline = advance.getDrawnAt().plusHours(request.coolingOffHours());
        if (Utc.now().isAfter(deadline)) {
            throw ApiException.unprocessable("COOLING_OFF_ELAPSED", "The cooling-off window has elapsed");
        }
        advance.setStatus(AdvanceStatus.CANCELLED.name());
        advance.setPrincipalResidual(Money.zero());
        advance.setFeeResidual(Money.zero());
        advanceRepository.save(advance);
        outboxWriter.loan(advance.getAdvanceId(), "ADVANCE_CANCELLED", Map.of("advanceId", advance.getAdvanceId().toString()));
        return views.advance(advance);
    }

    private Mandate resolveMandate(UUID mandateId, String subscriber) {
        if (mandateId != null) {
            Mandate mandate = mandateRepository.findById(mandateId)
                    .orElseThrow(() -> ApiException.notFound("MANDATE_NOT_FOUND", "Mandate not found"));
            if (!mandate.getSubscriberRef().equals(subscriber)) {
                throw ApiException.conflict("SUBSCRIBER_MISMATCH", "Mandate subscriber does not match the advance");
            }
            return mandate;
        }
        return mandateRepository.findBySubscriberRefAndStatus(subscriber, MandateStatus.ACTIVE.name()).stream()
                .findFirst()
                .orElseThrow(() -> ApiException.unprocessable("MANDATE_NOT_ACTIVE",
                        "Create-advance requires an active mandate for the subscriber"));
    }

    private void schedule(UUID advanceId, ScheduleEventType type, Map<String, Object> payload) {
        ScheduleEvent event = new ScheduleEvent();
        event.setEventId(Ids.uuidV7());
        event.setAdvanceId(advanceId);
        event.setEventType(type.name());
        try {
            event.setPayload(objectMapper.writeValueAsString(payload));
        } catch (Exception exception) {
            event.setPayload("{}");
        }
        event.setOccurredAt(Utc.now());
        scheduleEventRepository.save(event);
    }

    private Disclosure requireDisclosure(UUID id) {
        return disclosureRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("DISCLOSURE_NOT_FOUND", "Disclosure not found"));
    }

    private AdvanceAgreement requireAdvance(UUID id) {
        return advanceRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("ADVANCE_NOT_FOUND", "Advance not found"));
    }
}
