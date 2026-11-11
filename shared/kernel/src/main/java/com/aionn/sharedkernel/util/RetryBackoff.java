package com.aionn.sharedkernel.util;

public final class RetryBackoff {

    public static final int MAX_EXPONENT = 12;
    public static final long MAX_DELAY_SECONDS = 3600;

    private RetryBackoff() {
    }

    public static long seconds(int attempt) {
        int exponent = Math.min(Math.max(attempt, 0), MAX_EXPONENT);
        return Math.min(MAX_DELAY_SECONDS, 1L << exponent);
    }
}
