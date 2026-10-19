package com.lms.loan.repo;

import com.lms.loan.domain.entity.AdvanceAgreement;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdvanceRepository extends JpaRepository<AdvanceAgreement, UUID> {

    Optional<AdvanceAgreement> findByOriginationKey(String originationKey);

    List<AdvanceAgreement> findBySubscriberRefOrderByDueDateAscDrawnAtAscAdvanceIdAsc(String subscriberRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a from AdvanceAgreement a
            where a.subscriberRef = :subscriberRef and a.status in :statuses
            order by a.dueDate asc, a.drawnAt asc, a.advanceId asc
            """)
    List<AdvanceAgreement> lockBySubscriberAndStatus(@Param("subscriberRef") String subscriberRef,
                                                     @Param("statuses") Collection<String> statuses);

    List<AdvanceAgreement> findByStatusIn(Collection<String> statuses);
}
