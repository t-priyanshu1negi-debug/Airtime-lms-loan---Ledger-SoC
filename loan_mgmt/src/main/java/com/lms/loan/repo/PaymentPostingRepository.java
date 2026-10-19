package com.lms.loan.repo;

import com.lms.loan.domain.entity.PaymentPosting;
import com.lms.loan.domain.entity.PostingKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentPostingRepository extends JpaRepository<PaymentPosting, PostingKey> {

    List<PaymentPosting> findByIdempotencyKey(String idempotencyKey);

    List<PaymentPosting> findBySubscriberRefOrderByIdSettledAtAsc(String subscriberRef);
}
