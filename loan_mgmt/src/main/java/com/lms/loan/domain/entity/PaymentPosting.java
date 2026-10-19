package com.lms.loan.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "payment_posting")
public class PaymentPosting {

    @EmbeddedId
    private PostingKey id;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "subscriber_ref", nullable = false, length = 64)
    private String subscriberRef;

    @Column(name = "source", nullable = false, length = 16)
    private String source;

    @Column(name = "signal_id", length = 128)
    private String signalId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "authorised", nullable = false)
    private Boolean authorised;

    public PostingKey getId() { return id; }
    public void setId(PostingKey id) { this.id = id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getSubscriberRef() { return subscriberRef; }
    public void setSubscriberRef(String subscriberRef) { this.subscriberRef = subscriberRef; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSignalId() { return signalId; }
    public void setSignalId(String signalId) { this.signalId = signalId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Boolean getAuthorised() { return authorised; }
    public void setAuthorised(Boolean authorised) { this.authorised = authorised; }
}
