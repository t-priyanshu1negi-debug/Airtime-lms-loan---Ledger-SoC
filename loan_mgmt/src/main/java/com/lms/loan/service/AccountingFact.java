package com.lms.loan.service;

import com.lms.loan.domain.Money;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Economic fact for Reports. Account codes stay in ledger_mgmt. */
public final class AccountingFact {

    private AccountingFact() {
    }

    public static Map<String, Object> feeRevenue(UUID advanceId, String marketCode, String currency, BigDecimal fee) {
        return base(advanceId, "FEE_REVENUE", marketCode, currency, fee, Money.zero(), fee);
    }

    public static Map<String, Object> writeOff(UUID advanceId, String marketCode, String currency,
                                               BigDecimal principal, BigDecimal fee) {
        BigDecimal amount = Money.of(principal).add(Money.of(fee));
        return base(advanceId, "WRITE_OFF", marketCode, currency, fee, principal, amount);
    }

    public static Map<String, Object> recoveryOfWrittenOff(UUID advanceId, String marketCode, String currency,
                                                           BigDecimal amount) {
        return base(advanceId, "RECOVERY_OF_WRITTEN_OFF", marketCode, currency, Money.zero(), Money.zero(), amount);
    }

    private static Map<String, Object> base(UUID advanceId, String journalType, String marketCode, String currency,
                                            BigDecimal feeAmount, BigDecimal principalAmount, BigDecimal amount) {
        Map<String, Object> fact = new LinkedHashMap<>();
        fact.put("advanceId", advanceId.toString());
        fact.put("journalType", journalType);
        fact.put("marketCode", marketCode);
        fact.put("currency", currency);
        fact.put("feeAmount", Money.of(feeAmount));
        fact.put("principalAmount", Money.of(principalAmount));
        fact.put("amount", Money.of(amount));
        return fact;
    }
}
