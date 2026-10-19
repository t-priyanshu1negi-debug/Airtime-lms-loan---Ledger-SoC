package com.lms.loan.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "disclosure")
public class Disclosure {

    @Id
    @Column(name = "disclosure_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID disclosureId;

    @Column(name = "subscriber_ref", nullable = false, length = 64)
    private String subscriberRef;

    @Column(name = "advance_id", columnDefinition = "BINARY(16)")
    private UUID advanceId;

    @Column(name = "pid_version", nullable = false)
    private Integer pidVersion;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal fee;

    @Column(name = "total_repayable", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalRepayable;

    @Column(name = "rendered_text", nullable = false, length = 182)
    private String renderedText;

    @Column(name = "ack_token", nullable = false, length = 128)
    private String ackToken;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    public UUID getDisclosureId() { return disclosureId; }
    public void setDisclosureId(UUID disclosureId) { this.disclosureId = disclosureId; }
    public String getSubscriberRef() { return subscriberRef; }
    public void setSubscriberRef(String subscriberRef) { this.subscriberRef = subscriberRef; }
    public UUID getAdvanceId() { return advanceId; }
    public void setAdvanceId(UUID advanceId) { this.advanceId = advanceId; }
    public Integer getPidVersion() { return pidVersion; }
    public void setPidVersion(Integer pidVersion) { this.pidVersion = pidVersion; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }
    public BigDecimal getTotalRepayable() { return totalRepayable; }
    public void setTotalRepayable(BigDecimal totalRepayable) { this.totalRepayable = totalRepayable; }
    public String getRenderedText() { return renderedText; }
    public void setRenderedText(String renderedText) { this.renderedText = renderedText; }
    public String getAckToken() { return ackToken; }
    public void setAckToken(String ackToken) { this.ackToken = ackToken; }
    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
}
