package com.lms.loan.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class AttemptKey implements Serializable {

    @Column(name = "attempt_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID attemptId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AttemptKey() {
    }

    public AttemptKey(UUID attemptId, LocalDateTime createdAt) {
        this.attemptId = attemptId;
        this.createdAt = createdAt;
    }

    public UUID getAttemptId() { return attemptId; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AttemptKey that)) return false;
        return Objects.equals(attemptId, that.attemptId) && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attemptId, createdAt);
    }
}
