package com.lms.loan.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "recovery_attempt")
public class RecoveryAttempt {

    @EmbeddedId
    private AttemptKey id;

    @Column(name = "intent_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID intentId;

    @Column(name = "outcome", nullable = false, length = 24)
    private String outcome;

    @Column(name = "conduct_record", nullable = false, columnDefinition = "json")
    private String conductRecord;

    public AttemptKey getId() { return id; }
    public void setId(AttemptKey id) { this.id = id; }
    public UUID getIntentId() { return intentId; }
    public void setIntentId(UUID intentId) { this.intentId = intentId; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getConductRecord() { return conductRecord; }
    public void setConductRecord(String conductRecord) { this.conductRecord = conductRecord; }
}
