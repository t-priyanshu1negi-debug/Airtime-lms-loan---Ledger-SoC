package com.lms.loan.repo;

import com.lms.loan.domain.entity.DeductionIntent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeductionIntentRepository extends JpaRepository<DeductionIntent, UUID> {

    Optional<DeductionIntent> findBySignalId(String signalId);
}
