package com.lms.loan.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class PostingKey implements Serializable {

    @Column(name = "posting_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID postingId;

    @Column(name = "settled_at", nullable = false)
    private LocalDateTime settledAt;

    public PostingKey() {
    }

    public PostingKey(UUID postingId, LocalDateTime settledAt) {
        this.postingId = postingId;
        this.settledAt = settledAt;
    }

    public UUID getPostingId() { return postingId; }
    public LocalDateTime getSettledAt() { return settledAt; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PostingKey that)) return false;
        return Objects.equals(postingId, that.postingId) && Objects.equals(settledAt, that.settledAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postingId, settledAt);
    }
}
