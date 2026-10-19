package com.lms.loan.repo;

import com.lms.loan.domain.entity.AttemptKey;
import com.lms.loan.domain.entity.RecoveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecoveryAttemptRepository extends JpaRepository<RecoveryAttempt, AttemptKey> {

    List<RecoveryAttempt> findByIntentId(UUID intentId);
}
