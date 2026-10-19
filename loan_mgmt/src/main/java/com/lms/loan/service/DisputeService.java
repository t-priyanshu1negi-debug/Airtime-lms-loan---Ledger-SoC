package com.lms.loan.service;

import com.lms.loan.api.ApiException;
import com.lms.loan.api.Enums;
import com.lms.loan.api.dto.LoanDtos.DisputeView;
import com.lms.loan.api.dto.LoanDtos.RaiseDisputeRequest;
import com.lms.loan.api.dto.LoanDtos.TransitionDisputeRequest;
import com.lms.loan.domain.AdvanceStatus;
import com.lms.loan.domain.DisputeStatus;
import com.lms.loan.domain.DisputeType;
import com.lms.loan.domain.Ids;
import com.lms.loan.domain.Utc;
import com.lms.loan.domain.entity.AdvanceAgreement;
import com.lms.loan.domain.entity.Dispute;
import com.lms.loan.repo.AdvanceRepository;
import com.lms.loan.repo.DisputeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DisputeService {

    private static final Map<DisputeStatus, Set<DisputeStatus>> NEXT = Map.of(
            DisputeStatus.RAISED, EnumSet.of(DisputeStatus.UNDER_REVIEW),
            DisputeStatus.UNDER_REVIEW, EnumSet.of(DisputeStatus.UPHELD, DisputeStatus.REJECTED),
            DisputeStatus.UPHELD, EnumSet.of(DisputeStatus.CLOSED),
            DisputeStatus.REJECTED, EnumSet.of(DisputeStatus.CLOSED),
            DisputeStatus.CLOSED, EnumSet.noneOf(DisputeStatus.class));

    private final DisputeRepository disputeRepository;
    private final AdvanceRepository advanceRepository;
    private final OutboxWriter outboxWriter;
    private final LoanViews views;

    public DisputeService(DisputeRepository disputeRepository,
                          AdvanceRepository advanceRepository,
                          OutboxWriter outboxWriter,
                          LoanViews views) {
        this.disputeRepository = disputeRepository;
        this.advanceRepository = advanceRepository;
        this.outboxWriter = outboxWriter;
        this.views = views;
    }

    @Transactional
    public DisputeView raise(UUID advanceId, RaiseDisputeRequest request) {
        AdvanceAgreement advance = advanceRepository.findById(advanceId)
                .orElseThrow(() -> ApiException.notFound("ADVANCE_NOT_FOUND", "Advance not found"));
        AdvanceStatus status = AdvanceStatus.valueOf(advance.getStatus());
        if (status == AdvanceStatus.PENDING || status == AdvanceStatus.ABORTED || status == AdvanceStatus.CANCELLED) {
            throw ApiException.conflict("DISPUTE_NOT_ALLOWED", "A dispute cannot be raised on this advance state");
        }
        DisputeType type = Enums.parse(DisputeType.class, request.disputeType(), "DISPUTE_TYPE_INVALID");
        Dispute row = new Dispute();
        row.setDisputeId(Ids.uuidV7());
        row.setAdvanceId(advanceId);
        row.setDisputeType(type.name());
        row.setStatus(DisputeStatus.RAISED.name());
        row.setHoldActive(true);
        row.setRaisedAt(Utc.now());
        disputeRepository.save(row);
        outboxWriter.append(OutboxWriter.DISPUTE_EVENTS, advanceId.toString(), "DISPUTE_RAISED",
                Map.of("disputeId", row.getDisputeId().toString(), "advanceId", advanceId.toString(),
                        "disputeType", type.name(), "holdActive", true));
        return views.dispute(row);
    }

    @Transactional
    public DisputeView transition(UUID disputeId, TransitionDisputeRequest request) {
        Dispute row = disputeRepository.findById(disputeId)
                .orElseThrow(() -> ApiException.notFound("DISPUTE_NOT_FOUND", "Dispute not found"));
        DisputeStatus current = DisputeStatus.valueOf(row.getStatus());
        DisputeStatus target = Enums.parse(DisputeStatus.class, request.status(), "DISPUTE_STATUS_INVALID");
        if (!NEXT.get(current).contains(target)) {
            throw ApiException.conflict("ILLEGAL_TRANSITION", "Cannot move dispute from " + current + " to " + target);
        }
        row.setStatus(target.name());
        if (target == DisputeStatus.UPHELD || target == DisputeStatus.REJECTED || target == DisputeStatus.CLOSED) {
            row.setHoldActive(false);
            if (row.getResolvedAt() == null) {
                row.setResolvedAt(Utc.now());
            }
        }
        disputeRepository.save(row);
        outboxWriter.append(OutboxWriter.DISPUTE_EVENTS, row.getAdvanceId().toString(), "DISPUTE_UPDATED",
                Map.of("disputeId", row.getDisputeId().toString(), "advanceId", row.getAdvanceId().toString(),
                        "status", target.name(), "holdActive", Boolean.TRUE.equals(row.getHoldActive())));
        return views.dispute(row);
    }

    @Transactional(readOnly = true)
    public List<DisputeView> list(UUID advanceId) {
        if (!advanceRepository.existsById(advanceId)) {
            throw ApiException.notFound("ADVANCE_NOT_FOUND", "Advance not found");
        }
        return disputeRepository.findByAdvanceIdOrderByRaisedAtAsc(advanceId).stream().map(views::dispute).toList();
    }
}
