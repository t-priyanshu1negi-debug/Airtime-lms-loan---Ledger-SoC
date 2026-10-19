package com.lms.loan.repo;

import com.lms.loan.domain.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    List<Dispute> findByAdvanceIdOrderByRaisedAtAsc(UUID advanceId);

    boolean existsByAdvanceIdInAndHoldActiveTrue(Collection<UUID> advanceIds);
}
