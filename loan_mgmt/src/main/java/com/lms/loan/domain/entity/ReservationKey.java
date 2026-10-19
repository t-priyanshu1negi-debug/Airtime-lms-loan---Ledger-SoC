package com.lms.loan.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ReservationKey implements Serializable {

    @Column(name = "reservation_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID reservationId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ReservationKey() {
    }

    public ReservationKey(UUID reservationId, LocalDateTime createdAt) {
        this.reservationId = reservationId;
        this.createdAt = createdAt;
    }

    public UUID getReservationId() { return reservationId; }
    public void setReservationId(UUID reservationId) { this.reservationId = reservationId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ReservationKey that)) return false;
        return Objects.equals(reservationId, that.reservationId) && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservationId, createdAt);
    }
}
