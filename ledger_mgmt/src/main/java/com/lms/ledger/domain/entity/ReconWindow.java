package com.lms.ledger.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "recon_window")
public class ReconWindow {

    @Id
    @Column(name = "window_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID windowId;

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    @Column(name = "window_end", nullable = false)
    private LocalDateTime windowEnd;

    @Column(name = "quoted_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal quotedTotal;

    @Column(name = "ingested_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal ingestedTotal;

    @Column(name = "posted_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal postedTotal;

    @Column(name = "filtered_count", nullable = false)
    private Long filteredCount;

    @Column(name = "break_count", nullable = false)
    private Integer breakCount;

    @Column(name = "exception_owner", length = 64)
    private String exceptionOwner;

    public UUID getWindowId() { return windowId; }
    public void setWindowId(UUID windowId) { this.windowId = windowId; }
    public LocalDateTime getWindowStart() { return windowStart; }
    public void setWindowStart(LocalDateTime windowStart) { this.windowStart = windowStart; }
    public LocalDateTime getWindowEnd() { return windowEnd; }
    public void setWindowEnd(LocalDateTime windowEnd) { this.windowEnd = windowEnd; }
    public BigDecimal getQuotedTotal() { return quotedTotal; }
    public void setQuotedTotal(BigDecimal quotedTotal) { this.quotedTotal = quotedTotal; }
    public BigDecimal getIngestedTotal() { return ingestedTotal; }
    public void setIngestedTotal(BigDecimal ingestedTotal) { this.ingestedTotal = ingestedTotal; }
    public BigDecimal getPostedTotal() { return postedTotal; }
    public void setPostedTotal(BigDecimal postedTotal) { this.postedTotal = postedTotal; }
    public Long getFilteredCount() { return filteredCount; }
    public void setFilteredCount(Long filteredCount) { this.filteredCount = filteredCount; }
    public Integer getBreakCount() { return breakCount; }
    public void setBreakCount(Integer breakCount) { this.breakCount = breakCount; }
    public String getExceptionOwner() { return exceptionOwner; }
    public void setExceptionOwner(String exceptionOwner) { this.exceptionOwner = exceptionOwner; }
}
