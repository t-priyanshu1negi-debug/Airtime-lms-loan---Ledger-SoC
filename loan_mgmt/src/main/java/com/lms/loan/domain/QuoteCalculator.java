package com.lms.loan.domain;

import java.math.BigDecimal;

/**
 * Quote slice: min(max_share × gross, headroom, mandate_share × gross, residual).
 * A sub-floor remainder is absorbed so the sweep does not strand dust. The floor is
 * applied only when the caller supplies it. max_share is never invented here.
 */
public final class QuoteCalculator {

    private QuoteCalculator() {
    }

    public record Input(
            BigDecimal gross,
            BigDecimal maxShare,
            BigDecimal mandateShare,
            BigDecimal collectable,
            BigDecimal crossProductHeadroom,
            BigDecimal neverStrandFloor
    ) {
    }

    public record Result(BigDecimal quoted, String refusalCode) {
    }

    public static Result quote(Input input) {
        if (input.maxShare() == null) {
            return new Result(Money.zero(), "MAX_SHARE_UNSET");
        }
        BigDecimal collectable = Money.of(input.collectable());
        if (collectable.signum() <= 0 || input.gross() == null || input.gross().signum() <= 0) {
            return new Result(Money.zero(), "NOTHING_COLLECTABLE");
        }
        BigDecimal raw = collectable;
        raw = raw.min(Money.of(input.maxShare().multiply(input.gross())));
        raw = raw.min(Money.of(input.mandateShare().multiply(input.gross())));
        if (input.crossProductHeadroom() != null) {
            raw = raw.min(Money.of(input.crossProductHeadroom()));
        }
        raw = Money.of(raw);
        if (input.neverStrandFloor() != null) {
            BigDecimal floor = Money.of(input.neverStrandFloor());
            BigDecimal remainder = collectable.subtract(raw);
            if (remainder.signum() > 0 && remainder.compareTo(floor) < 0) {
                raw = collectable;
            }
        }
        if (raw.signum() <= 0) {
            return new Result(Money.zero(), "NOTHING_COLLECTABLE");
        }
        return new Result(raw, null);
    }
}
