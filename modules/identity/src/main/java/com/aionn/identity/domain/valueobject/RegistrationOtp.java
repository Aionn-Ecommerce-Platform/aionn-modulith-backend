package com.aionn.identity.domain.valueobject;

import com.aionn.sharedkernel.util.OtpGenerator;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class RegistrationOtp {

    private final String code;
    private final Instant resendAvailableAt;
    private final Instant expiredAt;

    private RegistrationOtp(String code, Instant resendAvailableAt, Instant expiredAt) {
        this.code = Objects.requireNonNull(code, "OTP code cannot be null");
        this.resendAvailableAt = Objects.requireNonNull(resendAvailableAt, "Resend available time cannot be null");
        this.expiredAt = Objects.requireNonNull(expiredAt, "Expiry time cannot be null");
    }

    public static RegistrationOtp generate(int resendCooldownSeconds, int expirySeconds) {
        return generate(resendCooldownSeconds, expirySeconds, Clock.systemUTC());
    }

    public static RegistrationOtp generate(int resendCooldownSeconds, int expirySeconds, Clock clock) {
        if (resendCooldownSeconds < 0) {
            throw new IllegalArgumentException("Resend cooldown seconds cannot be negative");
        }
        if (expirySeconds < 0) {
            throw new IllegalArgumentException("Expiry seconds cannot be negative");
        }

        String code = OtpGenerator.generate6DigitOtp();
        Instant now = clock.instant();
        Instant resendAvailableAt = now.plusSeconds(resendCooldownSeconds);
        Instant expiredAt = now.plusSeconds(expirySeconds);

        return new RegistrationOtp(code, resendAvailableAt, expiredAt);
    }

    public String getCode() {
        return code;
    }

    public Instant getResendAvailableAt() {
        return resendAvailableAt;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }

    public boolean isExpired() {
        return isExpired(Clock.systemUTC());
    }

    public boolean isExpired(Clock clock) {
        return !expiredAt.isAfter(clock.instant());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RegistrationOtp that = (RegistrationOtp) o;
        return Objects.equals(code, that.code) &&
                Objects.equals(resendAvailableAt, that.resendAvailableAt) &&
                Objects.equals(expiredAt, that.expiredAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, resendAvailableAt, expiredAt);
    }

    @Override
    public String toString() {
        return "RegistrationOtp{" +
                "code=***" +
                ", resendAvailableAt=" + resendAvailableAt +
                ", expiredAt=" + expiredAt +
                '}';
    }
}
