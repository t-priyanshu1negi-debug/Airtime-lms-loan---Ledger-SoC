package com.lms.loan.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "collection_reservation")
public class CollectionReservation {

    @EmbeddedId
    private ReservationKey id;

    @Column(name = "signal_id", nullable = false, length = 128)
    private String signalId;

    @Column(name = "subscriber_ref", nullable = false, length = 64)
    private String subscriberRef;

    @Column(name = "verb", nullable = false, length = 8)
    private String verb;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "refusal_code", length = 32)
    private String refusalCode;

    public ReservationKey getId() { return id; }
    public void setId(ReservationKey id) { this.id = id; }
    public String getSignalId() { return signalId; }
    public void setSignalId(String signalId) { this.signalId = signalId; }
    public String getSubscriberRef() { return subscriberRef; }
    public void setSubscriberRef(String subscriberRef) { this.subscriberRef = subscriberRef; }
    public String getVerb() { return verb; }
    public void setVerb(String verb) { this.verb = verb; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getRefusalCode() { return refusalCode; }
    public void setRefusalCode(String refusalCode) { this.refusalCode = refusalCode; }
}
