package com.lms.ledger.repo;

import com.lms.ledger.domain.entity.ProjectionWatermark;
import com.lms.ledger.domain.entity.WatermarkKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatermarkRepository extends JpaRepository<ProjectionWatermark, WatermarkKey> {

    List<ProjectionWatermark> findAllByOrderByIdProjectionNameAsc();
}
