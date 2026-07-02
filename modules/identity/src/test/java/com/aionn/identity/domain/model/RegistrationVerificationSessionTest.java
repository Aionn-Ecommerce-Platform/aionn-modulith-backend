package com.aionn.identity.domain.model;

import com.aionn.identity.domain.exception.IdentityException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
                LocalDateTime.now(Clock.systemUTC()).plusSeconds(60),
                LocalDateTime.now(Clock.systemUTC()).plusMinutes(5),
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
    void shouldRejectVerificationAfterSessionAlreadyVerified() {
        RegistrationVerificationSession session = new RegistrationVerificationSession(
                "reg-1",
                "+84987654321",
                "123456",
                0,
                5,
                LocalDateTime.now(Clock.systemUTC()).plusSeconds(60),
                LocalDateTime.now(Clock.systemUTC()).plusMinutes(5),
                false,
                null,
                null);

        session.verify("123456");

        assertThrows(IdentityException.class, () -> session.verify(null));
    }

    @Test
    void shouldFailWhenOtpAttemptsExceeded() {
        RegistrationVerificationSession session = new RegistrationVerificationSession(
                "reg-1",
                "+84987654321",
                "123456",
                0,
                1,
                LocalDateTime.now(Clock.systemUTC()).plusSeconds(60),
                LocalDateTime.now(Clock.systemUTC()).plusMinutes(5),
                false,
                null,
                null);

        assertThrows(IdentityException.class, () -> session.verify("999999"));
        assertTrue(session.isLocked());
        assertTrue(session.getExpiredAt().isAfter(LocalDateTime.now(Clock.systemUTC())));
    }

    @Test
    void shouldResetAttemptCountWhenResendingOtp() {
        RegistrationVerificationSession session = new RegistrationVerificationSession(
                "reg-1",
                "+84987654321",
                "123456",
                3,
                5,
                LocalDateTime.now(Clock.systemUTC()).minusSeconds(1),
                LocalDateTime.now(Clock.systemUTC()).plusMinutes(5),
                false,
                null,
                null);

        session.resend(
                "654321",
                LocalDateTime.now(Clock.systemUTC()).plusSeconds(60),
                LocalDateTime.now(Clock.systemUTC()).plusMinutes(5));

        assertEquals(0, session.getAttemptCount());
        assertEquals("654321", session.getOtpCode());
    }
}
