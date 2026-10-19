package com.lms.loan.repo;

import com.lms.loan.domain.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByTopicAndPublishedAtIsNullOrderByOutboxIdAsc(String topic);
}
