package com.lms.loan.repo;

import com.lms.loan.domain.entity.DunningState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DunningStateRepository extends JpaRepository<DunningState, UUID> {
}
