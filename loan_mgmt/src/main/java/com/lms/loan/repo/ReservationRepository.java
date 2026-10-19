package com.lms.loan.repo;

import com.lms.loan.domain.entity.CollectionReservation;
import com.lms.loan.domain.entity.ReservationKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<CollectionReservation, ReservationKey> {

    List<CollectionReservation> findBySignalIdOrderByIdCreatedAtAsc(String signalId);
}
