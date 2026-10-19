package com.lms.ledger.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class Support {

    private Support() {
    }

    public static BigDecimal money(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    public static UUID uuidV7() {
        long timestamp = System.currentTimeMillis() & 0x0000_FFFF_FFFF_FFFFL;
        long randomA = ThreadLocalRandom.current().nextLong() & 0x0FFFL;
        long most = (timestamp << 16) | (0x7L << 12) | randomA;
        long least = ThreadLocalRandom.current().nextLong();
        least = (least & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;
        return new UUID(most, least);
    }
}
