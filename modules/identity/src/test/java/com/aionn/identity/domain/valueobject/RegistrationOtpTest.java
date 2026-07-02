package com.aionn.identity.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationOtpTest {

    @Test
    void generate_shouldCreateOtpWithCorrectTimings() {
        int resendCooldownSeconds = 60;
        int expirySeconds = 300;
        LocalDateTime beforeGeneration = LocalDateTime.now();

        RegistrationOtp otp = RegistrationOtp.generate(resendCooldownSeconds, expirySeconds);

        assertNotNull(otp.getCode(), "OTP code should not be null");
        assertEquals(6, otp.getCode().length(), "OTP code should be 6 digits");
        assertTrue(otp.getCode().matches("\\d{6}"), "OTP code should contain only digits");

        assertNotNull(otp.getResendAvailableAt(), "Resend available time should not be null");
        assertNotNull(otp.getExpiredAt(), "Expiry time should not be null");

        // Verify timing is approximately correct (within 1 second tolerance)
        LocalDateTime expectedResendTime = beforeGeneration.plusSeconds(resendCooldownSeconds);
        LocalDateTime expectedExpiryTime = beforeGeneration.plusSeconds(expirySeconds);

        assertTrue(otp.getResendAvailableAt().isAfter(expectedResendTime.minusSeconds(1)),
                "Resend time should be approximately " + resendCooldownSeconds + " seconds from now");
        assertTrue(otp.getResendAvailableAt().isBefore(expectedResendTime.plusSeconds(1)),
                "Resend time should be approximately " + resendCooldownSeconds + " seconds from now");

        assertTrue(otp.getExpiredAt().isAfter(expectedExpiryTime.minusSeconds(1)),
                "Expiry time should be approximately " + expirySeconds + " seconds from now");
        assertTrue(otp.getExpiredAt().isBefore(expectedExpiryTime.plusSeconds(1)),
                "Expiry time should be approximately " + expirySeconds + " seconds from now");
    }

    @Test
    void generate_shouldThrowException_whenResendCooldownIsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> RegistrationOtp.generate(-1, 300),
                "Should throw exception for negative resend cooldown");
    }

    @Test
    void generate_shouldThrowException_whenExpirySecondsIsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> RegistrationOtp.generate(60, -1),
                "Should throw exception for negative expiry seconds");
    }

    @Test
    void isExpired_shouldReturnFalse_whenOtpIsNotExpired() {
        RegistrationOtp otp = RegistrationOtp.generate(60, 300);

        assertFalse(otp.isExpired(), "OTP should not be expired immediately after generation");
    }

    @Test
    void isExpired_shouldReturnTrue_whenOtpIsExpired() {
        RegistrationOtp otp = RegistrationOtp.generate(0, 0);

        assertTrue(otp.isExpired(), "OTP should be expired after expiry time has passed");
    }

    @Test
    void toString_shouldMaskOtpCode() {
        RegistrationOtp otp = RegistrationOtp.generate(60, 300);

        String otpString = otp.toString();

        assertFalse(otpString.contains(otp.getCode()), "toString should not expose the actual OTP code");
        assertTrue(otpString.contains("***"), "toString should mask the OTP code with ***");
    }

    @Test
    void equals_shouldReturnTrue_forSameValues() {
        RegistrationOtp otp1 = RegistrationOtp.generate(60, 300);
        RegistrationOtp otp2 = RegistrationOtp.generate(60, 300);

        assertNotEquals(otp1, otp2, "Different OTPs should not be equal");
        assertEquals(otp1, otp1, "Same OTP instance should be equal to itself");
    }

    @Test
    void hashCode_shouldBeConsistent() {
        RegistrationOtp otp = RegistrationOtp.generate(60, 300);

        int hashCode1 = otp.hashCode();
        int hashCode2 = otp.hashCode();

        assertEquals(hashCode1, hashCode2, "Hash code should be consistent");
    }
}
