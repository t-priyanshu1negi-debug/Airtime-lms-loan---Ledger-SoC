package com.lms.ledger.repo;

import com.lms.ledger.domain.entity.BilledKey;
import com.lms.ledger.domain.entity.BilledVsCollected;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BilledVsCollectedRepository extends JpaRepository<BilledVsCollected, BilledKey> {

    List<BilledVsCollected> findAllByOrderByIdBusinessDateAsc();

    List<BilledVsCollected> findByIdRailOrderByIdBusinessDateAsc(String rail);
}
