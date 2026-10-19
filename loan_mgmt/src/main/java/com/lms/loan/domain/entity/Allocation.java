package com.lms.loan.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "allocation")
public class Allocation {

    @EmbeddedId
    private AllocationKey id;

    @Column(name = "posting_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID postingId;

    @Column(name = "advance_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID advanceId;

    @Column(name = "component", nullable = false, length = 12)
    private String component;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "seq", nullable = false)
    private Short seq;

    public AllocationKey getId() { return id; }
    public void setId(AllocationKey id) { this.id = id; }
    public UUID getPostingId() { return postingId; }
    public void setPostingId(UUID postingId) { this.postingId = postingId; }
    public UUID getAdvanceId() { return advanceId; }
    public void setAdvanceId(UUID advanceId) { this.advanceId = advanceId; }
    public String getComponent() { return component; }
    public void setComponent(String component) { this.component = component; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Short getSeq() { return seq; }
    public void setSeq(Short seq) { this.seq = seq; }
}
