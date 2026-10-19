package com.lms.ledger.repo;

import com.lms.ledger.domain.entity.AuditChain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditChainRepository extends JpaRepository<AuditChain, Long> {

    List<AuditChain> findAllByOrderBySeqAsc();

    AuditChain findFirstByOrderBySeqDesc();
}
