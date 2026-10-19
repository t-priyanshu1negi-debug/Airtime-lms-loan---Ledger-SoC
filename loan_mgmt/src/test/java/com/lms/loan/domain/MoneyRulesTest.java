package com.lms.loan.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MoneyRulesTest {

    @Test
    void nestedAllocationMatchesTheWorkedExample() {
        UUID advanceA = UUID.fromString("01928a00-0000-7000-8000-00000000000a");
        UUID advanceB = UUID.fromString("01928a00-0000-7000-8000-00000000000b");
        List<AllocationEngine.Slice> slices = AllocationEngine.allocate(List.of(
                new AllocationEngine.Obligation(advanceB, LocalDate.of(2026, 10, 19),
                        LocalDateTime.of(2026, 10, 5, 18, 30, 6), new BigDecimal("5.0000"), new BigDecimal("30.0000")),
                new AllocationEngine.Obligation(advanceA, LocalDate.of(2026, 10, 15),
                        LocalDateTime.of(2026, 10, 1, 9, 0, 6), new BigDecimal("7.5000"), new BigDecimal("50.0000"))
        ), new BigDecimal("60"));

        assertEquals(3, slices.size());
        assertEquals(advanceA, slices.get(0).advanceId());
        assertEquals(AllocationComponent.FEE, slices.get(0).component());
        assertEquals(new BigDecimal("7.5000"), slices.get(0).amount());
        assertEquals(1, slices.get(0).seq());
        assertEquals(AllocationComponent.PRINCIPAL, slices.get(1).component());
        assertEquals(new BigDecimal("50.0000"), slices.get(1).amount());
        assertEquals(advanceB, slices.get(2).advanceId());
        assertEquals(AllocationComponent.FEE, slices.get(2).component());
        assertEquals(new BigDecimal("2.5000"), slices.get(2).amount());
        assertEquals(3, slices.get(2).seq());
    }

    @Test
    void quoteUsesTheNarrowerShareAndDoesNotInventAFloor() {
        QuoteCalculator.Result result = QuoteCalculator.quote(new QuoteCalculator.Input(
                new BigDecimal("150"),
                new BigDecimal("0.40"),
                new BigDecimal("0.40"),
                new BigDecimal("92.5000"),
                null,
                null));
        assertEquals(new BigDecimal("60.0000"), result.quoted());
        assertNull(result.refusalCode());
    }

    @Test
    void neverStrandFloorAbsorbsADustRemainder() {
        QuoteCalculator.Result result = QuoteCalculator.quote(new QuoteCalculator.Input(
                new BigDecimal("20"),
                new BigDecimal("0.40"),
                new BigDecimal("0.40"),
                new BigDecimal("10.0000"),
                null,
                new BigDecimal("5")));
        assertEquals(new BigDecimal("10.0000"), result.quoted());
    }

    @Test
    void agingBucketsAndDpd() {
        assertEquals(0, Aging.daysPastDue(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 15)));
        assertEquals(1, Aging.daysPastDue(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 16)));
        assertEquals(0, Aging.bucket(0));
        assertEquals(1, Aging.bucket(30));
        assertEquals(2, Aging.bucket(31));
        assertEquals(3, Aging.bucket(89));
        assertEquals(4, Aging.bucket(90));
    }

    @Test
    void uuidBinaryRoundTrip() {
        UUID id = UUID.fromString("01928a00-0000-7000-8000-00000000000a");
        UuidBinaryConverter converter = new UuidBinaryConverter();
        byte[] bytes = converter.convertToDatabaseColumn(id);
        assertEquals(16, bytes.length);
        assertEquals(id, converter.convertToEntityAttribute(bytes));
        assertEquals((byte) 0x01, bytes[0]);
        assertEquals((byte) 0x0a, bytes[15]);
    }
}
