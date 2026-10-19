package com.lms.ledger.repo;

import com.lms.ledger.domain.entity.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface JournalLineRepository extends JpaRepository<JournalLine, UUID> {

    List<JournalLine> findByJournalIdIn(Collection<UUID> journalIds);

    List<JournalLine> findByJournalId(UUID journalId);
}
