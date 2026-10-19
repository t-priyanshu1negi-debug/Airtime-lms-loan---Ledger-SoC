package com.lms.loan.repo;

import com.lms.loan.domain.entity.Mandate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MandateRepository extends JpaRepository<Mandate, UUID> {

    List<Mandate> findBySubscriberRefAndStatus(String subscriberRef, String status);

    List<Mandate> findBySubscriberRefOrderByArmedAtAsc(String subscriberRef);
}
