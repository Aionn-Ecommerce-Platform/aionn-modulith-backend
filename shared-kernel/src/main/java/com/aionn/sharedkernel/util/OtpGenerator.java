package com.aionn.sharedkernel.util;

import java.security.SecureRandom;

public final class OtpGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private OtpGenerator() {
    }

    public static String generateNumericOtp(int length) {
        if (length <= 0 || length > 9) {
            throw new IllegalArgumentException("OTP length must be between 1 and 9");
        }
        int max = (int) Math.pow(10, length);
        String format = "%0" + length + "d";
        return String.format(format, SECURE_RANDOM.nextInt(max));
    }

    public static String generate6DigitOtp() {
        return generateNumericOtp(6);
    }

    public static String generate8DigitOtp() {
        return generateNumericOtp(8);
    }
}
