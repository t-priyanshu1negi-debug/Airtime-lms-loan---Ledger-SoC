package com.lms.ledger.domain;

import java.math.BigDecimal;
import java.util.List;

public final class JournalBalancer {

    private JournalBalancer() {
    }

    public static void requireBalanced(List<Chart.Line> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("A journal needs at least one line");
        }
        BigDecimal debit = Support.zero();
        BigDecimal credit = Support.zero();
        for (Chart.Line line : lines) {
            if (!"DR".equals(line.side()) && !"CR".equals(line.side())) {
                throw new IllegalArgumentException("Side must be DR or CR");
            }
            if (line.amount() == null || line.amount().signum() <= 0) {
                throw new IllegalArgumentException("Line amount must be positive");
            }
            if ("DR".equals(line.side())) {
                debit = debit.add(line.amount());
            } else {
                credit = credit.add(line.amount());
            }
        }
        if (debit.compareTo(credit) != 0) {
            throw new IllegalArgumentException("Debits " + debit + " do not equal credits " + credit);
        }
    }
}
