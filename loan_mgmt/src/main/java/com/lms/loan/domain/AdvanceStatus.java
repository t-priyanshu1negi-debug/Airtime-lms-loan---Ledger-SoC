package com.lms.loan.domain;

public enum AdvanceStatus {
    PENDING,
    DISBURSED,
    PARTIALLY_RECOVERED,
    RECOVERED,
    EXPIRED,
    WRITTEN_OFF,
    CANCELLED,
    ABORTED;

    public boolean collectable() {
        return this == DISBURSED || this == PARTIALLY_RECOVERED || this == EXPIRED || this == WRITTEN_OFF;
    }
}
