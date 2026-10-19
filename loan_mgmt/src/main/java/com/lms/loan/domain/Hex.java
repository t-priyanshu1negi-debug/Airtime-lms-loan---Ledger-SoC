package com.lms.loan.domain;

import java.util.HexFormat;

public final class Hex {

    private Hex() {
    }

    public static byte[] decode32(String hex) {
        if (hex == null) {
            return null;
        }
        String trimmed = hex.trim();
        if (trimmed.length() != 64) {
            throw new IllegalArgumentException("expected 32-byte hex");
        }
        return HexFormat.of().parseHex(trimmed);
    }

    public static String encode(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return HexFormat.of().formatHex(bytes);
    }
}
