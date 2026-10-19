package com.lms.ledger.repo;

import com.lms.ledger.domain.entity.Journal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalRepository extends JpaRepository<Journal, UUID> {

    Optional<Journal> findBySourceEventId(String sourceEventId);

    List<Journal> findByAdvanceIdOrderByPostedAtAsc(UUID advanceId);

    List<Journal> findByAdvanceIdInOrderByPostedAtAsc(Collection<UUID> advanceIds);

    List<Journal> findByMarketCodeOrderByPostedAtAsc(String marketCode);

    List<Journal> findAllByOrderByPostedAtAsc();
}
