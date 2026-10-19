package com.lms.loan.service;

import com.lms.loan.api.ApiException;
import com.lms.loan.api.dto.LoanDtos.AdvanceView;
import com.lms.loan.api.dto.LoanDtos.MandateView;
import com.lms.loan.api.dto.LoanDtos.OutboxView;
import com.lms.loan.api.dto.LoanDtos.ScheduleView;
import com.lms.loan.api.dto.LoanDtos.SubscriberContext;
import com.lms.loan.domain.Utc;
import com.lms.loan.domain.entity.AdvanceAgreement;
import com.lms.loan.domain.entity.OutboxEvent;
import com.lms.loan.repo.AdvanceRepository;
import com.lms.loan.repo.MandateRepository;
import com.lms.loan.repo.OutboxRepository;
import com.lms.loan.repo.ScheduleEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LoanQueryService {

    private final AdvanceRepository advanceRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final MandateRepository mandateRepository;
    private final OutboxRepository outboxRepository;
    private final LoanViews views;

    public LoanQueryService(AdvanceRepository advanceRepository,
                            ScheduleEventRepository scheduleEventRepository,
                            MandateRepository mandateRepository,
                            OutboxRepository outboxRepository,
                            LoanViews views) {
        this.advanceRepository = advanceRepository;
        this.scheduleEventRepository = scheduleEventRepository;
        this.mandateRepository = mandateRepository;
        this.outboxRepository = outboxRepository;
        this.views = views;
    }

    @Transactional(readOnly = true)
    public AdvanceView advance(UUID advanceId) {
        return views.advance(require(advanceId));
    }

    @Transactional(readOnly = true)
    public List<AdvanceView> advances(String subscriberRef) {
        return advanceRepository.findBySubscriberRefOrderByDueDateAscDrawnAtAscAdvanceIdAsc(subscriberRef).stream()
                .map(views::advance)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduleView> schedule(UUID advanceId) {
        require(advanceId);
        return scheduleEventRepository.findByAdvanceIdOrderByOccurredAtAsc(advanceId).stream().map(views::schedule).toList();
    }

    @Transactional(readOnly = true)
    public List<MandateView> mandates(String subscriberRef) {
        return mandateRepository.findBySubscriberRefOrderByArmedAtAsc(subscriberRef).stream().map(views::mandate).toList();
    }

    @Transactional(readOnly = true)
    public SubscriberContext context(String subscriberRef) {
        return new SubscriberContext(subscriberRef, advances(subscriberRef));
    }

    @Transactional(readOnly = true)
    public List<OutboxView> unpublished(String topic) {
        return outboxRepository.findByTopicAndPublishedAtIsNullOrderByOutboxIdAsc(topic).stream()
                .map(views::outbox)
                .toList();
    }

    @Transactional
    public void markPublished(long outboxId) {
        OutboxEvent event = outboxRepository.findById(outboxId)
                .orElseThrow(() -> ApiException.notFound("OUTBOX_NOT_FOUND", "Outbox row not found"));
        if (event.getPublishedAt() == null) {
            event.setPublishedAt(Utc.now());
            outboxRepository.save(event);
        }
    }

    private AdvanceAgreement require(UUID advanceId) {
        return advanceRepository.findById(advanceId)
                .orElseThrow(() -> ApiException.notFound("ADVANCE_NOT_FOUND", "Advance not found"));
    }
}
