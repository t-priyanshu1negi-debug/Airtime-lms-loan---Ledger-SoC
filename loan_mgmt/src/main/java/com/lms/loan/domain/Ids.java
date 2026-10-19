package com.lms.loan.domain;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class Ids {

    private Ids() {
    }

    /** RFC 9562 UUIDv7. The advance primary key is a UUIDv7. */
    public static UUID uuidV7() {
        long timestamp = System.currentTimeMillis() & 0x0000_FFFF_FFFF_FFFFL;
        long randomA = ThreadLocalRandom.current().nextLong() & 0x0FFFL;
        long most = (timestamp << 16) | (0x7L << 12) | randomA;
        long least = ThreadLocalRandom.current().nextLong();
        least = (least & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;
        return new UUID(most, least);
    }
}
