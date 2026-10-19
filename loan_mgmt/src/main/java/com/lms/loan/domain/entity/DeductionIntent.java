package com.lms.loan.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deduction_intent")
public class DeductionIntent {

    @Id
    @Column(name = "intent_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID intentId;

    @Column(name = "signal_id", nullable = false, length = 128)
    private String signalId;

    @Column(name = "subscriber_ref", nullable = false, length = 64)
    private String subscriberRef;

    @Column(name = "gross_recharge", nullable = false, precision = 19, scale = 4)
    private BigDecimal grossRecharge;

    @Column(name = "quoted_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal quotedAmount;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UUID getIntentId() { return intentId; }
    public void setIntentId(UUID intentId) { this.intentId = intentId; }
    public String getSignalId() { return signalId; }
    public void setSignalId(String signalId) { this.signalId = signalId; }
    public String getSubscriberRef() { return subscriberRef; }
    public void setSubscriberRef(String subscriberRef) { this.subscriberRef = subscriberRef; }
    public BigDecimal getGrossRecharge() { return grossRecharge; }
    public void setGrossRecharge(BigDecimal grossRecharge) { this.grossRecharge = grossRecharge; }
    public BigDecimal getQuotedAmount() { return quotedAmount; }
    public void setQuotedAmount(BigDecimal quotedAmount) { this.quotedAmount = quotedAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
