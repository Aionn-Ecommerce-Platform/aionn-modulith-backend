package com.aionn.sharedkernel.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class OtpGeneratorPropertyTest {

    @Provide
    Arbitrary<Integer> validLengths() {
        return Arbitraries.integers().between(1, 9);
    }

    @Provide
    Arbitrary<Integer> invalidLengths() {
        return Arbitraries.oneOf(
                Arbitraries.integers().lessOrEqual(0),
                Arbitraries.integers().greaterOrEqual(10));
    }

    @Property(tries = 100)
    void property22_validLengthYieldsDigitsOnly(@ForAll("validLengths") int length) {
        String otp = OtpGenerator.generateNumericOtp(length);

        assertEquals(length, otp.length(),
                () -> "OTP \"" + otp + "\" should have length " + length);
        assertTrue(otp.chars().allMatch(c -> c >= '0' && c <= '9'),
                () -> "OTP \"" + otp + "\" should contain digits 0-9 only");
    }

    @Property(tries = 100)
    void property22_invalidLengthThrowsIae(@ForAll("invalidLengths") int length) {
        assertThrows(IllegalArgumentException.class,
                () -> OtpGenerator.generateNumericOtp(length));
    }
}
