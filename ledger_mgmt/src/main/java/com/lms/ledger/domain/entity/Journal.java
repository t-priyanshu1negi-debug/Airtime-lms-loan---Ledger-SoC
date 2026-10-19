package com.lms.ledger.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "journal")
public class Journal {

    @Id
    @Column(name = "journal_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID journalId;

    @Column(name = "source_event_id", nullable = false, length = 128)
    private String sourceEventId;

    @Column(name = "advance_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID advanceId;

    @Column(name = "journal_type", nullable = false, length = 32)
    private String journalType;

    @Column(name = "market_code", nullable = false, length = 8)
    private String marketCode;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "posted_at", nullable = false)
    private LocalDateTime postedAt;

    public UUID getJournalId() { return journalId; }
    public void setJournalId(UUID journalId) { this.journalId = journalId; }
    public String getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(String sourceEventId) { this.sourceEventId = sourceEventId; }
    public UUID getAdvanceId() { return advanceId; }
    public void setAdvanceId(UUID advanceId) { this.advanceId = advanceId; }
    public String getJournalType() { return journalType; }
    public void setJournalType(String journalType) { this.journalType = journalType; }
    public String getMarketCode() { return marketCode; }
    public void setMarketCode(String marketCode) { this.marketCode = marketCode; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDateTime getPostedAt() { return postedAt; }
    public void setPostedAt(LocalDateTime postedAt) { this.postedAt = postedAt; }
}
