package com.lms.loan.api;

import com.lms.loan.domain.Hex;

import java.util.Arrays;

public final class Enums {

    private Enums() {
    }

    public static <E extends Enum<E>> E parse(Class<E> type, String raw, String code) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.badRequest(code, type.getSimpleName() + " is required");
        }
        try {
            return Enum.valueOf(type, raw.trim());
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest(code, "Unknown " + type.getSimpleName() + " " + raw
                    + ". Allowed: " + Arrays.toString(type.getEnumConstants()));
        }
    }

    public static byte[] msisdnHash(String hex) {
        try {
            byte[] bytes = Hex.decode32(hex);
            if (bytes == null) {
                throw ApiException.badRequest("MSISDN_HASH_INVALID", "msisdnHashHex must be 64 hex characters");
            }
            return bytes;
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("MSISDN_HASH_INVALID", "msisdnHashHex must be 64 hex characters");
        }
    }
}
