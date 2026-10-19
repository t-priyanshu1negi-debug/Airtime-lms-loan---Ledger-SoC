package com.lms.ledger.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "billed_vs_collected")
public class BilledVsCollected {

    @EmbeddedId
    private BilledKey id;

    @Column(name = "billed", nullable = false, precision = 19, scale = 4)
    private BigDecimal billed;

    @Column(name = "collected", nullable = false, precision = 19, scale = 4)
    private BigDecimal collected;

    public BilledKey getId() { return id; }
    public void setId(BilledKey id) { this.id = id; }
    public BigDecimal getBilled() { return billed; }
    public void setBilled(BigDecimal billed) { this.billed = billed; }
    public BigDecimal getCollected() { return collected; }
    public void setCollected(BigDecimal collected) { this.collected = collected; }
}
