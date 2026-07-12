package com.aionn.identity.domain.model;

import com.aionn.identity.domain.exception.IdentityException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrationVerificationSessionTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    private RegistrationVerificationSession session(int attemptCount, int maxAttempts,
            Instant resendAvailableAt, Instant expiredAt) {
        return new RegistrationVerificationSession(
                "reg-1",
                "+84987654321",
                "123456",
                attemptCount,
                maxAttempts,
                resendAvailableAt,
                expiredAt,
                false,
                null,
                null);
    }

    @Test
    void verifyMarksSessionVerifiedWhenOtpCorrect() {
        RegistrationVerificationSession s = session(0, 5,
                FIXED_INSTANT.plusSeconds(60), FIXED_INSTANT.plusSeconds(300));

        s.verify("123456", FIXED_CLOCK);

        assertThat(s.isVerified()).isTrue();
        assertThat(s.getOtpCode()).isNull();
        assertThat(s.getVerificationToken()).isNotNull();
        assertThat(s.getVerifiedAt()).isEqualTo(FIXED_INSTANT);
    }

    @Test
    void verifyRejectsReVerificationAfterSuccess() {
        RegistrationVerificationSession s = session(0, 5,
                FIXED_INSTANT.plusSeconds(60), FIXED_INSTANT.plusSeconds(300));
        s.verify("123456", FIXED_CLOCK);

        assertThatThrownBy(() -> s.verify(null, FIXED_CLOCK)).isInstanceOf(IdentityException.class);
    }

    @Test
    void verifyLocksSessionWhenAttemptsExceeded() {
        RegistrationVerificationSession s = session(0, 1,
                FIXED_INSTANT.plusSeconds(60), FIXED_INSTANT.plusSeconds(300));

        assertThatThrownBy(() -> s.verify("999999", FIXED_CLOCK)).isInstanceOf(IdentityException.class);
        assertThat(s.isLocked()).isTrue();
    }

    @Test
    void verifyRejectsWhenExpired() {
        RegistrationVerificationSession s = session(0, 5,
                FIXED_INSTANT.minusSeconds(120), FIXED_INSTANT.minusSeconds(60));

        assertThatThrownBy(() -> s.verify("123456", FIXED_CLOCK)).isInstanceOf(IdentityException.class);
    }

    @Test
    void verifyIncrementsAttemptWithoutLockingWhenAttemptsRemain() {
        RegistrationVerificationSession s = session(0, 5,
                FIXED_INSTANT.plusSeconds(60), FIXED_INSTANT.plusSeconds(300));

        assertThatThrownBy(() -> s.verify("000000", FIXED_CLOCK)).isInstanceOf(IdentityException.class);
        assertThat(s.getAttemptCount()).isEqualTo(1);
        assertThat(s.isLocked()).isFalse();
    }

    @Test
    void verifyRejectsWhenAlreadyLocked() {
        RegistrationVerificationSession s = session(5, 5,
                FIXED_INSTANT.plusSeconds(60), FIXED_INSTANT.plusSeconds(300));

        assertThatThrownBy(() -> s.verify("123456", FIXED_CLOCK)).isInstanceOf(IdentityException.class);
    }

    @Test
    void resendResetsAttemptCountAndSwapsOtp() {
        RegistrationVerificationSession s = session(3, 5,
                FIXED_INSTANT.minusSeconds(1), FIXED_INSTANT.plusSeconds(300));

        s.resend("654321",
                FIXED_INSTANT.plusSeconds(60),
                FIXED_INSTANT.plusSeconds(300),
                FIXED_CLOCK);

        assertThat(s.getAttemptCount()).isZero();
        assertThat(s.getOtpCode()).isEqualTo("654321");
    }

    @Test
    void resendRejectedAfterVerified() {
        RegistrationVerificationSession s = session(0, 5,
                FIXED_INSTANT.minusSeconds(1), FIXED_INSTANT.plusSeconds(300));
        s.verify("123456", FIXED_CLOCK);

        assertThatThrownBy(() -> s.resend("654321",
                FIXED_INSTANT.plusSeconds(60),
                FIXED_INSTANT.plusSeconds(300),
                FIXED_CLOCK))
                .isInstanceOf(IdentityException.class);
    }

    @Test
    void resendRejectedWhenExpired() {
        RegistrationVerificationSession s = session(0, 5,
                FIXED_INSTANT.minusSeconds(120), FIXED_INSTANT.minusSeconds(60));

        assertThatThrownBy(() -> s.resend("654321",
                FIXED_INSTANT.plusSeconds(60),
                FIXED_INSTANT.plusSeconds(300),
                FIXED_CLOCK))
                .isInstanceOf(IdentityException.class);
    }

    @Test
    void resendRejectedWhenCooldownNotElapsed() {
        RegistrationVerificationSession s = session(0, 5,
                FIXED_INSTANT.plusSeconds(60), FIXED_INSTANT.plusSeconds(300));

        assertThatThrownBy(() -> s.resend("654321",
                FIXED_INSTANT.plusSeconds(60),
                FIXED_INSTANT.plusSeconds(300),
                FIXED_CLOCK))
                .isInstanceOf(IdentityException.class);
    }
}
