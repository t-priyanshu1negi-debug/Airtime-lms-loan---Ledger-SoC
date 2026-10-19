package com.lms.ledger.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "projection_watermark")
public class ProjectionWatermark {

    @EmbeddedId
    private WatermarkKey id;

    @Column(name = "last_offset", nullable = false)
    private Long lastOffset;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public WatermarkKey getId() { return id; }
    public void setId(WatermarkKey id) { this.id = id; }
    public Long getLastOffset() { return lastOffset; }
    public void setLastOffset(Long lastOffset) { this.lastOffset = lastOffset; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
