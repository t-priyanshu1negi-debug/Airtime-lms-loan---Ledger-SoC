package com.lms.loan.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dispute")
public class Dispute {

    @Id
    @Column(name = "dispute_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID disputeId;

    @Column(name = "advance_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID advanceId;

    @Column(name = "dispute_type", nullable = false, length = 24)
    private String disputeType;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "hold_active", nullable = false)
    private Boolean holdActive;

    @Column(name = "raised_at", nullable = false)
    private LocalDateTime raisedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public UUID getDisputeId() { return disputeId; }
    public void setDisputeId(UUID disputeId) { this.disputeId = disputeId; }
    public UUID getAdvanceId() { return advanceId; }
    public void setAdvanceId(UUID advanceId) { this.advanceId = advanceId; }
    public String getDisputeType() { return disputeType; }
    public void setDisputeType(String disputeType) { this.disputeType = disputeType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getHoldActive() { return holdActive; }
    public void setHoldActive(Boolean holdActive) { this.holdActive = holdActive; }
    public LocalDateTime getRaisedAt() { return raisedAt; }
    public void setRaisedAt(LocalDateTime raisedAt) { this.raisedAt = raisedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
