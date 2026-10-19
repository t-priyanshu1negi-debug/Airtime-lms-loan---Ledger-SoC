package com.lms.loan.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dunning_state")
public class DunningState {

    @Id
    @Column(name = "advance_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID advanceId;

    @Column(name = "days_past_due", nullable = false)
    private Integer daysPastDue;

    @Column(name = "aging_bucket", nullable = false)
    private Integer agingBucket;

    @Column(name = "ladder_step", nullable = false)
    private Integer ladderStep;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UUID getAdvanceId() { return advanceId; }
    public void setAdvanceId(UUID advanceId) { this.advanceId = advanceId; }
    public Integer getDaysPastDue() { return daysPastDue; }
    public void setDaysPastDue(Integer daysPastDue) { this.daysPastDue = daysPastDue; }
    public Integer getAgingBucket() { return agingBucket; }
    public void setAgingBucket(Integer agingBucket) { this.agingBucket = agingBucket; }
    public Integer getLadderStep() { return ladderStep; }
    public void setLadderStep(Integer ladderStep) { this.ladderStep = ladderStep; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
