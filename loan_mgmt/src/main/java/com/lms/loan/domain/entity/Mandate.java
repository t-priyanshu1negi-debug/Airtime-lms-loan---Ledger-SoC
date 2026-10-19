package com.lms.loan.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mandate")
public class Mandate {

    @Id
    @Column(name = "mandate_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID mandateId;

    @Column(name = "subscriber_ref", nullable = false, length = 64)
    private String subscriberRef;

    @Column(name = "disclosure_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID disclosureId;

    @Column(name = "mandate_share", nullable = false, precision = 7, scale = 6)
    private BigDecimal mandateShare;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "armed_at", nullable = false)
    private LocalDateTime armedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public UUID getMandateId() { return mandateId; }
    public void setMandateId(UUID mandateId) { this.mandateId = mandateId; }
    public String getSubscriberRef() { return subscriberRef; }
    public void setSubscriberRef(String subscriberRef) { this.subscriberRef = subscriberRef; }
    public UUID getDisclosureId() { return disclosureId; }
    public void setDisclosureId(UUID disclosureId) { this.disclosureId = disclosureId; }
    public BigDecimal getMandateShare() { return mandateShare; }
    public void setMandateShare(BigDecimal mandateShare) { this.mandateShare = mandateShare; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getArmedAt() { return armedAt; }
    public void setArmedAt(LocalDateTime armedAt) { this.armedAt = armedAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
}
