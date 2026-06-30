package com.aionn.sharedkernel.util;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;

public final class IdGenerator {

    private IdGenerator() {
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final char[] ENCODING = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    private static final int[] DECODE = new int[128];

    static {
        Arrays.fill(DECODE, -1);
        for (int i = 0; i < ENCODING.length; i++) {
            DECODE[ENCODING[i]] = i;
        }
    }

    public static String ulid() {
        return ulid(Instant.now().toEpochMilli());
    }

    public static String ulid(long timestampMs) {
        char[] ulid = new char[26];

        ulid[0] = ENCODING[(int) ((timestampMs >>> 45) & 0x1F)];
        ulid[1] = ENCODING[(int) ((timestampMs >>> 40) & 0x1F)];
        ulid[2] = ENCODING[(int) ((timestampMs >>> 35) & 0x1F)];
        ulid[3] = ENCODING[(int) ((timestampMs >>> 30) & 0x1F)];
        ulid[4] = ENCODING[(int) ((timestampMs >>> 25) & 0x1F)];
        ulid[5] = ENCODING[(int) ((timestampMs >>> 20) & 0x1F)];
        ulid[6] = ENCODING[(int) ((timestampMs >>> 15) & 0x1F)];
        ulid[7] = ENCODING[(int) ((timestampMs >>> 10) & 0x1F)];
        ulid[8] = ENCODING[(int) ((timestampMs >>> 5) & 0x1F)];
        ulid[9] = ENCODING[(int) ((timestampMs) & 0x1F)];

        byte[] randomBytes = new byte[10];
        RANDOM.nextBytes(randomBytes);
        ulid[10] = ENCODING[(randomBytes[0] & 0xFF) >>> 3];
        ulid[11] = ENCODING[((randomBytes[0] & 0x07) << 2) | ((randomBytes[1] & 0xFF) >>> 6)];
        ulid[12] = ENCODING[(randomBytes[1] & 0x3E) >>> 1];
        ulid[13] = ENCODING[((randomBytes[1] & 0x01) << 4) | ((randomBytes[2] & 0xFF) >>> 4)];
        ulid[14] = ENCODING[((randomBytes[2] & 0x0F) << 1) | ((randomBytes[3] & 0xFF) >>> 7)];
        ulid[15] = ENCODING[(randomBytes[3] & 0x7C) >>> 2];
        ulid[16] = ENCODING[((randomBytes[3] & 0x03) << 3) | ((randomBytes[4] & 0xFF) >>> 5)];
        ulid[17] = ENCODING[randomBytes[4] & 0x1F];
        ulid[18] = ENCODING[(randomBytes[5] & 0xFF) >>> 3];
        ulid[19] = ENCODING[((randomBytes[5] & 0x07) << 2) | ((randomBytes[6] & 0xFF) >>> 6)];
        ulid[20] = ENCODING[(randomBytes[6] & 0x3E) >>> 1];
        ulid[21] = ENCODING[((randomBytes[6] & 0x01) << 4) | ((randomBytes[7] & 0xFF) >>> 4)];
        ulid[22] = ENCODING[((randomBytes[7] & 0x0F) << 1) | ((randomBytes[8] & 0xFF) >>> 7)];
        ulid[23] = ENCODING[(randomBytes[8] & 0x7C) >>> 2];
        ulid[24] = ENCODING[((randomBytes[8] & 0x03) << 3) | ((randomBytes[9] & 0xFF) >>> 5)];
        ulid[25] = ENCODING[randomBytes[9] & 0x1F];

        return new String(ulid);
    }

    public static boolean isValid(String value) {
        if (value == null || value.length() != 26) {
            return false;
        }
        for (int i = 0; i < 26; i++) {
            char c = value.charAt(i);
            if (c >= DECODE.length || DECODE[c] == -1) {
                return false;
            }
        }
        return true;
    }

    public static long extractTimestamp(String ulid) {
        if (ulid == null || ulid.length() != 26) {
            throw new IllegalArgumentException("Invalid ULID: " + ulid);
        }
        long timestamp = 0;
        for (int i = 0; i < 10; i++) {
            timestamp = (timestamp << 5) | indexOf(ulid.charAt(i));
        }
        return timestamp;
    }

    private static int indexOf(char c) {
        if (c >= DECODE.length || DECODE[c] == -1) {
            throw new IllegalArgumentException("Invalid ULID character: " + c);
        }
        return DECODE[c];
    }
}
