package com.lms.ledger.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class BilledKey implements Serializable {

    @Column(name = "rail", nullable = false, length = 32)
    private String rail;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    public BilledKey() {
    }

    public BilledKey(String rail, LocalDate businessDate) {
        this.rail = rail;
        this.businessDate = businessDate;
    }

    public String getRail() { return rail; }
    public LocalDate getBusinessDate() { return businessDate; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BilledKey that)) return false;
        return Objects.equals(rail, that.rail) && Objects.equals(businessDate, that.businessDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rail, businessDate);
    }
}
