package com.lms.loan.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    public static final int SCALE = 4;

    private Money() {
    }

    public static BigDecimal of(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    public static boolean isZero(BigDecimal value) {
        return value == null || value.signum() == 0;
    }
}
