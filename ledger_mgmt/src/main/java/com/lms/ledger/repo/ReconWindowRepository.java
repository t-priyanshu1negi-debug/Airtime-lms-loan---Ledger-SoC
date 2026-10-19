package com.lms.ledger.repo;

import com.lms.ledger.domain.entity.ReconWindow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReconWindowRepository extends JpaRepository<ReconWindow, UUID> {

    List<ReconWindow> findAllByOrderByWindowStartAsc();
}
