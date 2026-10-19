package com.lms.loan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.loan.api.dto.LoanDtos.DunningTickRequest;
import com.lms.loan.api.dto.LoanDtos.DunningTickView;
import com.lms.loan.domain.AdvanceStatus;
import com.lms.loan.domain.Aging;
import com.lms.loan.domain.Ids;
import com.lms.loan.domain.Money;
import com.lms.loan.domain.ScheduleEventType;
import com.lms.loan.domain.Utc;
import com.lms.loan.domain.entity.AdvanceAgreement;
import com.lms.loan.domain.entity.DunningState;
import com.lms.loan.domain.entity.ScheduleEvent;
import com.lms.loan.repo.AdvanceRepository;
import com.lms.loan.repo.DisputeRepository;
import com.lms.loan.repo.DunningStateRepository;
import com.lms.loan.repo.ScheduleEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DunningService {

    private static final List<String> TICKED = List.of(
            AdvanceStatus.DISBURSED.name(),
            AdvanceStatus.PARTIALLY_RECOVERED.name(),
            AdvanceStatus.EXPIRED.name(),
            AdvanceStatus.WRITTEN_OFF.name());

    private final AdvanceRepository advanceRepository;
    private final DunningStateRepository dunningStateRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final DisputeRepository disputeRepository;
    private final OutboxWriter outboxWriter;
    private final ObjectMapper objectMapper;

    public DunningService(AdvanceRepository advanceRepository,
                          DunningStateRepository dunningStateRepository,
                          ScheduleEventRepository scheduleEventRepository,
                          DisputeRepository disputeRepository,
                          OutboxWriter outboxWriter,
                          ObjectMapper objectMapper) {
        this.advanceRepository = advanceRepository;
        this.dunningStateRepository = dunningStateRepository;
        this.scheduleEventRepository = scheduleEventRepository;
        this.disputeRepository = disputeRepository;
        this.outboxWriter = outboxWriter;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DunningTickView tick(DunningTickRequest request) {
        LocalDateTime asOf = request.asOf() == null ? Utc.now() : request.asOf();
        int examined = 0;
        int graceEntered = 0;
        int expired = 0;
        int writtenOff = 0;
        List<UUID> writtenOffIds = new ArrayList<>();
        for (AdvanceAgreement advance : advanceRepository.findByStatusIn(TICKED)) {
            examined++;
            DunningState state = dunningStateRepository.findById(advance.getAdvanceId()).orElseGet(() -> {
                DunningState created = new DunningState();
                created.setAdvanceId(advance.getAdvanceId());
                created.setDaysPastDue(0);
                created.setAgingBucket(0);
                created.setLadderStep(0);
                return created;
            });
            int dpd = Aging.daysPastDue(advance.getDueDate(), asOf.toLocalDate());
            int bucket = Aging.bucket(dpd);
            boolean changed = state.getDaysPastDue() == null || state.getDaysPastDue() != dpd || state.getAgingBucket() != bucket;
            state.setDaysPastDue(dpd);
            state.setAgingBucket(bucket);
            state.setLadderStep(bucket);
            state.setUpdatedAt(asOf);
            dunningStateRepository.save(state);
            if (changed) {
                event(advance.getAdvanceId(), ScheduleEventType.DPD_TICK, Map.of("daysPastDue", dpd, "agingBucket", bucket), asOf);
            }
            boolean alreadyWrittenOff = AdvanceStatus.WRITTEN_OFF.name().equals(advance.getStatus());
            if (!alreadyWrittenOff && dpd > 0
                    && !scheduleEventRepository.existsByAdvanceIdAndEventType(advance.getAdvanceId(), ScheduleEventType.GRACE_ENTERED.name())) {
                event(advance.getAdvanceId(), ScheduleEventType.GRACE_ENTERED, Map.of("graceDatetime", advance.getGraceDatetime().toString()), asOf);
                graceEntered++;
            }
            if (!alreadyWrittenOff && asOf.isAfter(advance.getGraceDatetime())
                    && (AdvanceStatus.DISBURSED.name().equals(advance.getStatus())
                    || AdvanceStatus.PARTIALLY_RECOVERED.name().equals(advance.getStatus()))) {
                advance.setStatus(AdvanceStatus.EXPIRED.name());
                advanceRepository.save(advance);
                if (!scheduleEventRepository.existsByAdvanceIdAndEventType(advance.getAdvanceId(), ScheduleEventType.EXPIRED.name())) {
                    event(advance.getAdvanceId(), ScheduleEventType.EXPIRED, Map.of("graceDatetime", advance.getGraceDatetime().toString()), asOf);
                }
                expired++;
                outboxWriter.loan(advance.getAdvanceId(), "ADVANCE_EXPIRED", Map.of("advanceId", advance.getAdvanceId().toString()));
            }
            BigDecimal residual = Money.of(advance.getPrincipalResidual()).add(Money.of(advance.getFeeResidual()));
            boolean held = disputeRepository.existsByAdvanceIdInAndHoldActiveTrue(List.of(advance.getAdvanceId()));
            if (!alreadyWrittenOff && !held && dpd >= request.writeOffDays() && residual.signum() > 0) {
                advance.setStatus(AdvanceStatus.WRITTEN_OFF.name());
                advance.setWrittenOffAt(asOf);
                advanceRepository.save(advance);
                event(advance.getAdvanceId(), ScheduleEventType.WRITTEN_OFF,
                        Map.of("daysPastDue", dpd, "writeOffDays", request.writeOffDays()), asOf);
                outboxWriter.loan(advance.getAdvanceId(), "ADVANCE_WRITTEN_OFF", Map.of("advanceId", advance.getAdvanceId().toString()));
                outboxWriter.accounting(advance.getAdvanceId(), "WRITE_OFF", AccountingFact.writeOff(
                        advance.getAdvanceId(), advance.getMarketCode(), advance.getCurrency(),
                        advance.getPrincipalResidual(), advance.getFeeResidual()));
                writtenOff++;
                writtenOffIds.add(advance.getAdvanceId());
            }
        }
        return new DunningTickView(examined, graceEntered, expired, writtenOff, List.copyOf(writtenOffIds));
    }

    private void event(UUID advanceId, ScheduleEventType type, Map<String, Object> payload, LocalDateTime occurredAt) {
        ScheduleEvent row = new ScheduleEvent();
        row.setEventId(Ids.uuidV7());
        row.setAdvanceId(advanceId);
        row.setEventType(type.name());
        try {
            row.setPayload(objectMapper.writeValueAsString(payload));
        } catch (Exception exception) {
            row.setPayload("{}");
        }
        row.setOccurredAt(occurredAt);
        scheduleEventRepository.save(row);
    }
}
