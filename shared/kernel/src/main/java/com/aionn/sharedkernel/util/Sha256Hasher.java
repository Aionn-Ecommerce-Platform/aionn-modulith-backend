package com.aionn.sharedkernel.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Sha256Hasher {

    private static final HexFormat HEX = HexFormat.of();

    private Sha256Hasher() {
    }

    // Lowercase-hex SHA-256 of the UTF-8 bytes of {@code value}. Used across
    // Redis stores as a fixed-length, non-reversible key so raw tokens never
    // hit persistence, and by application services that need to derive the
    // same lookup hash on their side.
    public static String hexDigest(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HEX.formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
