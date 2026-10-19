package com.lms.ledger.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Chart of accounts lives in the ledger service, not in loan management.
 * Fee revenue uses the same accounts as the sample journals: FEE_RECEIVABLE / FEE_INCOME.
 */
public final class Chart {

    public static final String FEE_RECEIVABLE = "FEE_RECEIVABLE";
    public static final String FEE_INCOME = "FEE_INCOME";
    public static final String PRINCIPAL_RECEIVABLE = "PRINCIPAL_RECEIVABLE";
    public static final String WRITE_OFF_EXPENSE = "WRITE_OFF_EXPENSE";
    public static final String CASH_RECOVERY = "CASH_RECOVERY";
    public static final String RECOVERY_OF_WRITE_OFF = "RECOVERY_OF_WRITE_OFF";

    private Chart() {
    }

    public record Line(String accountCode, String side, BigDecimal amount) {
    }

    public static List<Line> lines(String journalType, BigDecimal feeAmount, BigDecimal principalAmount, BigDecimal amount) {
        BigDecimal fee = Support.money(feeAmount == null ? Support.zero() : feeAmount);
        BigDecimal principal = Support.money(principalAmount == null ? Support.zero() : principalAmount);
        BigDecimal total = Support.money(amount == null ? Support.zero() : amount);
        List<Line> lines = new ArrayList<>();
        switch (journalType) {
            case "FEE_REVENUE" -> addPair(lines, FEE_RECEIVABLE, FEE_INCOME, fee.signum() > 0 ? fee : total);
            case "WRITE_OFF" -> {
                BigDecimal writeOff = principal.add(fee);
                if (writeOff.signum() > 0) {
                    lines.add(new Line(WRITE_OFF_EXPENSE, "DR", writeOff));
                }
                if (principal.signum() > 0) {
                    lines.add(new Line(PRINCIPAL_RECEIVABLE, "CR", principal));
                }
                if (fee.signum() > 0) {
                    lines.add(new Line(FEE_RECEIVABLE, "CR", fee));
                }
            }
            case "RECOVERY_OF_WRITTEN_OFF" -> addPair(lines, CASH_RECOVERY, RECOVERY_OF_WRITE_OFF, total);
            default -> throw new IllegalArgumentException("Unsupported journal type " + journalType);
        }
        JournalBalancer.requireBalanced(lines);
        return List.copyOf(lines);
    }

    private static void addPair(List<Line> lines, String debit, String credit, BigDecimal amount) {
        if (amount.signum() <= 0) {
            return;
        }
        lines.add(new Line(debit, "DR", amount));
        lines.add(new Line(credit, "CR", amount));
    }
}
