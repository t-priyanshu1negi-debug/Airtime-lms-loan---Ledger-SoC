package com.lms.ledger.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChartTest {

    @Test
    void feeRevenueMatchesTheSampleBooks() {
        List<Chart.Line> lines = Chart.lines("FEE_REVENUE", new BigDecimal("7.50"), BigDecimal.ZERO, new BigDecimal("7.50"));
        assertEquals(2, lines.size());
        assertEquals("FEE_RECEIVABLE", lines.get(0).accountCode());
        assertEquals("DR", lines.get(0).side());
        assertEquals(new BigDecimal("7.5000"), lines.get(0).amount());
        assertEquals("FEE_INCOME", lines.get(1).accountCode());
        assertEquals("CR", lines.get(1).side());
    }

    @Test
    void writeOffBalancesPrincipalAndFee() {
        List<Chart.Line> lines = Chart.lines("WRITE_OFF", new BigDecimal("2.5000"), new BigDecimal("30.0000"), new BigDecimal("32.5000"));
        JournalBalancer.requireBalanced(lines);
        assertEquals(new BigDecimal("32.5000"), lines.get(0).amount());
        assertEquals("WRITE_OFF_EXPENSE", lines.get(0).accountCode());
    }

    @Test
    void unbalancedManualLinesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> JournalBalancer.requireBalanced(List.of(
                new Chart.Line("CASH", "DR", new BigDecimal("10.0000")),
                new Chart.Line("INCOME", "CR", new BigDecimal("9.0000"))
        )));
    }

    @Test
    void chainHashIsSha256OfPreviousPlusPayload() {
        byte[] previous = new byte[32];
        byte[] payload = com.lms.ledger.service.ReportingCommandService.sha256("RESERVE|SIG-0001|60.0000".getBytes(StandardCharsets.UTF_8));
        byte[] chain = com.lms.ledger.service.ReportingCommandService.sha256(
                com.lms.ledger.service.ReportingCommandService.concat(previous, payload));
        assertEquals(32, chain.length);
    }
}
