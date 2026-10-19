package com.lms.ledger.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "journal_line")
public class JournalLine {

    @Id
    @Column(name = "line_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID lineId;

    @Column(name = "journal_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID journalId;

    @Column(name = "account_code", nullable = false, length = 32)
    private String accountCode;

    @Column(name = "side", nullable = false, length = 2)
    private String side;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    public UUID getLineId() { return lineId; }
    public void setLineId(UUID lineId) { this.lineId = lineId; }
    public UUID getJournalId() { return journalId; }
    public void setJournalId(UUID journalId) { this.journalId = journalId; }
    public String getAccountCode() { return accountCode; }
    public void setAccountCode(String accountCode) { this.accountCode = accountCode; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
