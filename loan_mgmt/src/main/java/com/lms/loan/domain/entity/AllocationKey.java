package com.lms.loan.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class AllocationKey implements Serializable {

    @Column(name = "allocation_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID allocationId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AllocationKey() {
    }

    public AllocationKey(UUID allocationId, LocalDateTime createdAt) {
        this.allocationId = allocationId;
        this.createdAt = createdAt;
    }

    public UUID getAllocationId() { return allocationId; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AllocationKey that)) return false;
        return Objects.equals(allocationId, that.allocationId) && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allocationId, createdAt);
    }
}
