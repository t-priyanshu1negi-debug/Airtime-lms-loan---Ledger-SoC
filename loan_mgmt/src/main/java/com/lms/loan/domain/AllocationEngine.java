package com.lms.loan.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Nested waterfall: earliest due, then draw timestamp, then id.
 * Inside an advance, fee is retired before principal. Due date is an ordering key, not a gate.
 */
public final class AllocationEngine {

    private AllocationEngine() {
    }

    public record Obligation(
            UUID advanceId,
            LocalDate dueDate,
            LocalDateTime drawnAt,
            BigDecimal feeResidual,
            BigDecimal principalResidual
    ) {
    }

    public record Slice(UUID advanceId, AllocationComponent component, BigDecimal amount, int seq) {
    }

    public static List<Slice> allocate(List<Obligation> obligations, BigDecimal amount) {
        List<Obligation> ordered = new ArrayList<>(obligations);
        ordered.sort(Comparator.comparing(Obligation::dueDate)
                .thenComparing(Obligation::drawnAt)
                .thenComparing(Obligation::advanceId));

        BigDecimal left = Money.of(amount);
        List<Slice> slices = new ArrayList<>();
        int seq = 1;
        for (Obligation obligation : ordered) {
            if (left.signum() == 0) {
                break;
            }
            BigDecimal feeTake = left.min(Money.of(obligation.feeResidual()));
            if (feeTake.signum() > 0) {
                slices.add(new Slice(obligation.advanceId(), AllocationComponent.FEE, feeTake, seq++));
                left = left.subtract(feeTake);
            }
            if (left.signum() == 0) {
                break;
            }
            BigDecimal principalTake = left.min(Money.of(obligation.principalResidual()));
            if (principalTake.signum() > 0) {
                slices.add(new Slice(obligation.advanceId(), AllocationComponent.PRINCIPAL, principalTake, seq++));
                left = left.subtract(principalTake);
            }
        }
        return List.copyOf(slices);
    }
}
