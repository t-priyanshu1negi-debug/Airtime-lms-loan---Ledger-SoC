package com.lms.loan.repo;

import com.lms.loan.domain.entity.Disclosure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DisclosureRepository extends JpaRepository<Disclosure, UUID> {

    Optional<Disclosure> findByAckToken(String ackToken);
}
