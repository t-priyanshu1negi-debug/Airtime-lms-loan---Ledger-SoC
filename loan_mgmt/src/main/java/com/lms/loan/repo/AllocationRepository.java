package com.lms.loan.repo;

import com.lms.loan.domain.entity.Allocation;
import com.lms.loan.domain.entity.AllocationKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AllocationRepository extends JpaRepository<Allocation, AllocationKey> {

    List<Allocation> findByPostingIdOrderBySeqAsc(UUID postingId);

    @Query("""
            select a from Allocation a
            where a.advanceId in :advanceIds
            order by a.id.createdAt asc, a.seq asc
            """)
    List<Allocation> findForAdvances(@Param("advanceIds") Collection<UUID> advanceIds);

    @Query("""
            select a from Allocation a
            where a.advanceId = :advanceId
            order by a.id.createdAt asc, a.seq asc
            """)
    List<Allocation> findForAdvance(@Param("advanceId") UUID advanceId);

    boolean existsByAdvanceId(UUID advanceId);
}
