package com.lms.loan.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Five aging buckets. The HLD requires the buckets and does not publish the day edges.
 * Edges used here: 0 current, 1 = 1–30, 2 = 31–60, 3 = 61–89, 4 = 90+.
 * Write-off day count is supplied by the caller; it is not taken from the bucket edge.
 */
public final class Aging {

    private Aging() {
    }

    public static int daysPastDue(LocalDate dueDate, LocalDate asOf) {
        long days = ChronoUnit.DAYS.between(dueDate, asOf);
        return (int) Math.max(0, days);
    }

    public static int bucket(int daysPastDue) {
        if (daysPastDue <= 0) {
            return 0;
        }
        if (daysPastDue <= 30) {
            return 1;
        }
        if (daysPastDue <= 60) {
            return 2;
        }
        if (daysPastDue <= 89) {
            return 3;
        }
        return 4;
    }
}
