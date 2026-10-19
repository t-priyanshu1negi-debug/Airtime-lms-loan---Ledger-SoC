package com.lms.loan.repo;

import com.lms.loan.domain.entity.ScheduleEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScheduleEventRepository extends JpaRepository<ScheduleEvent, UUID> {

    List<ScheduleEvent> findByAdvanceIdOrderByOccurredAtAsc(UUID advanceId);

    boolean existsByAdvanceIdAndEventType(UUID advanceId, String eventType);
}
