package com.aionn.identity.domain.model;

import com.aionn.identity.domain.exception.IdentityException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationVerificationSessionTest {

    @Test
    void shouldMarkSessionAsVerifiedWhenOtpIsCorrect() {
        RegistrationVerificationSession session = new RegistrationVerificationSession(
                "reg-1",
                "+84987654321",
                "123456",
                0,
                5,
                LocalDateTime.now().plusSeconds(60),
                LocalDateTime.now().plusMinutes(5),
                false,
                null,
                null);

        session.verify("123456");

        assertTrue(session.isVerified());
        assertNull(session.getOtpCode());
        assertNotNull(session.getVerificationToken());
        assertNotNull(session.getVerifiedAt());
    }

    @Test
    void shouldFailWhenOtpAttemptsExceeded() {
        RegistrationVerificationSession session = new RegistrationVerificationSession(
                "reg-1",
                "+84987654321",
                "123456",
                0,
                1,
                LocalDateTime.now().plusSeconds(60),
                LocalDateTime.now().plusMinutes(5),
                false,
                null,
                null);

        assertThrows(IdentityException.class, () -> session.verify("999999"));
    }

    @Test
    void shouldResetAttemptCountWhenResendingOtp() {
        RegistrationVerificationSession session = new RegistrationVerificationSession(
                "reg-1",
                "+84987654321",
                "123456",
                3,
                5,
                LocalDateTime.now().minusSeconds(1),
                LocalDateTime.now().plusMinutes(5),
                false,
                null,
                null);

        session.resend("654321", LocalDateTime.now().plusSeconds(60), LocalDateTime.now().plusMinutes(5));

        assertTrue(session.getAttemptCount() == 0);
        assertTrue("654321".equals(session.getOtpCode()));
    }
}


