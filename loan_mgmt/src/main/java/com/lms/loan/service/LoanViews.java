package com.lms.loan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.loan.api.ApiException;
import com.lms.loan.api.dto.LoanDtos.AdvanceView;
import com.lms.loan.api.dto.LoanDtos.AllocationView;
import com.lms.loan.api.dto.LoanDtos.AttemptView;
import com.lms.loan.api.dto.LoanDtos.DisclosureView;
import com.lms.loan.api.dto.LoanDtos.DisputeView;
import com.lms.loan.api.dto.LoanDtos.DunningView;
import com.lms.loan.api.dto.LoanDtos.MandateView;
import com.lms.loan.api.dto.LoanDtos.OutboxView;
import com.lms.loan.api.dto.LoanDtos.ReservationView;
import com.lms.loan.api.dto.LoanDtos.ScheduleView;
import com.lms.loan.domain.Hex;
import com.lms.loan.domain.Money;
import com.lms.loan.domain.entity.AdvanceAgreement;
import com.lms.loan.domain.entity.Allocation;
import com.lms.loan.domain.entity.CollectionReservation;
import com.lms.loan.domain.entity.DeductionIntent;
import com.lms.loan.domain.entity.Disclosure;
import com.lms.loan.domain.entity.Dispute;
import com.lms.loan.domain.entity.DunningState;
import com.lms.loan.domain.entity.Mandate;
import com.lms.loan.domain.entity.OutboxEvent;
import com.lms.loan.domain.entity.RecoveryAttempt;
import com.lms.loan.domain.entity.ScheduleEvent;
import com.lms.loan.repo.AllocationRepository;
import com.lms.loan.repo.DunningStateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LoanViews {

    private final AllocationRepository allocationRepository;
    private final DunningStateRepository dunningStateRepository;
    private final ObjectMapper objectMapper;

    public LoanViews(AllocationRepository allocationRepository,
                     DunningStateRepository dunningStateRepository,
                     ObjectMapper objectMapper) {
        this.allocationRepository = allocationRepository;
        this.dunningStateRepository = dunningStateRepository;
        this.objectMapper = objectMapper;
    }

    public DisclosureView disclosure(Disclosure row) {
        return new DisclosureView(
                row.getDisclosureId(), row.getSubscriberRef(), row.getAdvanceId(), row.getPidVersion(),
                Money.of(row.getAmount()), Money.of(row.getFee()), Money.of(row.getTotalRepayable()),
                row.getRenderedText(), row.getAckToken(), row.getAcknowledgedAt());
    }

    public MandateView mandate(Mandate row) {
        return new MandateView(row.getMandateId(), row.getSubscriberRef(), row.getDisclosureId(),
                row.getMandateShare(), row.getStatus(), row.getArmedAt(), row.getRevokedAt());
    }

    public AdvanceView advance(AdvanceAgreement row) {
        DunningView dunning = dunningStateRepository.findById(row.getAdvanceId())
                .map(this::dunning)
                .orElse(null);
        List<AllocationView> allocations = allocationRepository.findForAdvance(row.getAdvanceId()).stream()
                .map(this::allocation)
                .toList();
        return new AdvanceView(
                row.getAdvanceId(), row.getMarketCode(), row.getSubscriberRef(), Hex.encode(row.getMsisdnHash()),
                row.getMsisdnValidFrom(), row.getOriginationKey(), row.getProductType(), row.getSkuId(),
                row.getPidId(), row.getPidVersion(), row.getCurrency(), row.getLenderOfRecord(),
                row.getRegulatedFlag(), Money.of(row.getPrincipalAmount()), Money.of(row.getFeeAmount()),
                row.getFeeMethod(), Money.of(row.getPrincipalResidual()), Money.of(row.getFeeResidual()),
                row.getStatus(), row.getDrawnAt(), row.getDueDate(), row.getGraceDatetime(), row.getWrittenOffAt(),
                row.getVersion(), dunning, allocations);
    }

    public AdvanceView advance(AdvanceAgreement row, List<Allocation> allocations, DunningState dunning) {
        return new AdvanceView(
                row.getAdvanceId(), row.getMarketCode(), row.getSubscriberRef(), Hex.encode(row.getMsisdnHash()),
                row.getMsisdnValidFrom(), row.getOriginationKey(), row.getProductType(), row.getSkuId(),
                row.getPidId(), row.getPidVersion(), row.getCurrency(), row.getLenderOfRecord(),
                row.getRegulatedFlag(), Money.of(row.getPrincipalAmount()), Money.of(row.getFeeAmount()),
                row.getFeeMethod(), Money.of(row.getPrincipalResidual()), Money.of(row.getFeeResidual()),
                row.getStatus(), row.getDrawnAt(), row.getDueDate(), row.getGraceDatetime(), row.getWrittenOffAt(),
                row.getVersion(), dunning == null ? null : this.dunning(dunning),
                allocations.stream().map(this::allocation).toList());
    }

    public AllocationView allocation(Allocation row) {
        return new AllocationView(row.getId().getAllocationId(), row.getPostingId(), row.getAdvanceId(),
                row.getComponent(), Money.of(row.getAmount()), row.getSeq(), row.getId().getCreatedAt());
    }

    public ScheduleView schedule(ScheduleEvent row) {
        return new ScheduleView(row.getEventId(), row.getAdvanceId(), row.getEventType(), row.getPayload(), row.getOccurredAt());
    }

    public DisputeView dispute(Dispute row) {
        return new DisputeView(row.getDisputeId(), row.getAdvanceId(), row.getDisputeType(), row.getStatus(),
                Boolean.TRUE.equals(row.getHoldActive()), row.getRaisedAt(), row.getResolvedAt());
    }

    public ReservationView reservation(CollectionReservation row) {
        return new ReservationView(row.getId().getReservationId(), row.getSignalId(), row.getVerb(),
                Money.of(row.getAmount()), row.getCurrency(), row.getRefusalCode(), row.getId().getCreatedAt());
    }

    public AttemptView attempt(RecoveryAttempt row) {
        return new AttemptView(row.getId().getAttemptId(), row.getIntentId(), row.getOutcome(),
                row.getConductRecord(), row.getId().getCreatedAt());
    }

    public OutboxView outbox(OutboxEvent row) {
        return new OutboxView(row.getOutboxId(), row.getTopic(), row.getMessageKey(), row.getEventType(),
                readTree(row.getPayload()), row.getCreatedAt());
    }

    public String subscriberOf(DeductionIntent intent) {
        return intent.getSubscriberRef();
    }

    private DunningView dunning(DunningState row) {
        return new DunningView(row.getDaysPastDue(), row.getAgingBucket(), row.getLadderStep(), row.getUpdatedAt());
    }

    private JsonNode readTree(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PAYLOAD_FAILED", "Stored outbox payload is not JSON");
        }
    }
}
