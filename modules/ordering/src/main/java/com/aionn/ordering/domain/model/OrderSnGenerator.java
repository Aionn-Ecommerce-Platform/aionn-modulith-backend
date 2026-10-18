package com.aionn.ordering.domain.model;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class OrderSnGenerator {

    // Crockford-style uppercase alphanumeric characters without confusing 0/O, 1/I
    private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd").withZone(ZoneOffset.UTC);

    private OrderSnGenerator() {
    }

    /**
     * Generates a Shopee-style order serial number (e.g. 261018TP4DTFP8).
     * Format: YYMMDD (6-digit date) + 8-character uppercase alphanumeric code.
     */
    public static String generate(Instant now) {
        String datePart = DATE_FMT.format(now != null ? now : Instant.now());
        char[] randomPart = new char[8];
        for (int i = 0; i < 8; i++) {
            randomPart[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return datePart + new String(randomPart);
    }
}

