package com.lms.loan.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.lms.loan.domain.Utc;

@Entity
@Table(name = "advance_agreement")
public class AdvanceAgreement {

    @Id
    @Column(name = "advance_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID advanceId;

    @Column(name = "market_code", nullable = false, length = 8)
    private String marketCode;

    @Column(name = "subscriber_ref", nullable = false, length = 64)
    private String subscriberRef;

    @Column(name = "msisdn_hash", nullable = false, columnDefinition = "BINARY(32)")
    private byte[] msisdnHash;

    @Column(name = "msisdn_valid_from", nullable = false)
    private LocalDateTime msisdnValidFrom;

    @Column(name = "origination_key", nullable = false, length = 128)
    private String originationKey;

    @Column(name = "product_type", nullable = false, length = 16)
    private String productType;

    @Column(name = "sku_id", nullable = false, length = 64)
    private String skuId;

    @Column(name = "pid_id", nullable = false, length = 64)
    private String pidId;

    @Column(name = "pid_version", nullable = false)
    private Integer pidVersion;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "lender_of_record", nullable = false, length = 64)
    private String lenderOfRecord;

    @Column(name = "regulated_flag", nullable = false)
    private Boolean regulatedFlag;

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalAmount;

    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal feeAmount;

    @Column(name = "fee_method", nullable = false, length = 16)
    private String feeMethod;

    @Column(name = "principal_residual", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalResidual;

    @Column(name = "fee_residual", nullable = false, precision = 19, scale = 4)
    private BigDecimal feeResidual;

    @Column(name = "status", nullable = false, length = 24)
    private String status;

    @Column(name = "drawn_at", nullable = false)
    private LocalDateTime drawnAt;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "grace_datetime", nullable = false)
    private LocalDateTime graceDatetime;

    @Column(name = "written_off_at")
    private LocalDateTime writtenOffAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = Utc.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Utc.now();
    }

    public UUID getAdvanceId() { return advanceId; }
    public void setAdvanceId(UUID advanceId) { this.advanceId = advanceId; }
    public String getMarketCode() { return marketCode; }
    public void setMarketCode(String marketCode) { this.marketCode = marketCode; }
    public String getSubscriberRef() { return subscriberRef; }
    public void setSubscriberRef(String subscriberRef) { this.subscriberRef = subscriberRef; }
    public byte[] getMsisdnHash() { return msisdnHash; }
    public void setMsisdnHash(byte[] msisdnHash) { this.msisdnHash = msisdnHash; }
    public LocalDateTime getMsisdnValidFrom() { return msisdnValidFrom; }
    public void setMsisdnValidFrom(LocalDateTime msisdnValidFrom) { this.msisdnValidFrom = msisdnValidFrom; }
    public String getOriginationKey() { return originationKey; }
    public void setOriginationKey(String originationKey) { this.originationKey = originationKey; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public String getSkuId() { return skuId; }
    public void setSkuId(String skuId) { this.skuId = skuId; }
    public String getPidId() { return pidId; }
    public void setPidId(String pidId) { this.pidId = pidId; }
    public Integer getPidVersion() { return pidVersion; }
    public void setPidVersion(Integer pidVersion) { this.pidVersion = pidVersion; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getLenderOfRecord() { return lenderOfRecord; }
    public void setLenderOfRecord(String lenderOfRecord) { this.lenderOfRecord = lenderOfRecord; }
    public Boolean getRegulatedFlag() { return regulatedFlag; }
    public void setRegulatedFlag(Boolean regulatedFlag) { this.regulatedFlag = regulatedFlag; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
    public String getFeeMethod() { return feeMethod; }
    public void setFeeMethod(String feeMethod) { this.feeMethod = feeMethod; }
    public BigDecimal getPrincipalResidual() { return principalResidual; }
    public void setPrincipalResidual(BigDecimal principalResidual) { this.principalResidual = principalResidual; }
    public BigDecimal getFeeResidual() { return feeResidual; }
    public void setFeeResidual(BigDecimal feeResidual) { this.feeResidual = feeResidual; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getDrawnAt() { return drawnAt; }
    public void setDrawnAt(LocalDateTime drawnAt) { this.drawnAt = drawnAt; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getGraceDatetime() { return graceDatetime; }
    public void setGraceDatetime(LocalDateTime graceDatetime) { this.graceDatetime = graceDatetime; }
    public LocalDateTime getWrittenOffAt() { return writtenOffAt; }
    public void setWrittenOffAt(LocalDateTime writtenOffAt) { this.writtenOffAt = writtenOffAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
