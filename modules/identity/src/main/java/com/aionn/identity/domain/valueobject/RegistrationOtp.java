package com.aionn.identity.domain.valueobject;

import com.aionn.sharedkernel.util.OtpGenerator;

import java.time.LocalDateTime;
import java.util.Objects;

public final class RegistrationOtp {

    private final String code;
    private final LocalDateTime resendAvailableAt;
    private final LocalDateTime expiredAt;

    private RegistrationOtp(String code, LocalDateTime resendAvailableAt, LocalDateTime expiredAt) {
        this.code = Objects.requireNonNull(code, "OTP code cannot be null");
        this.resendAvailableAt = Objects.requireNonNull(resendAvailableAt, "Resend available time cannot be null");
        this.expiredAt = Objects.requireNonNull(expiredAt, "Expiry time cannot be null");
    }

    public static RegistrationOtp generate(int resendCooldownSeconds, int expirySeconds) {
        if (resendCooldownSeconds < 0) {
            throw new IllegalArgumentException("Resend cooldown seconds cannot be negative");
        }
        if (expirySeconds < 0) {
            throw new IllegalArgumentException("Expiry seconds cannot be negative");
        }

        String code = OtpGenerator.generate6DigitOtp();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime resendAvailableAt = now.plusSeconds(resendCooldownSeconds);
        LocalDateTime expiredAt = now.plusSeconds(expirySeconds);

        return new RegistrationOtp(code, resendAvailableAt, expiredAt);
    }

    public String getCode() {
        return code;
    }

    public LocalDateTime getResendAvailableAt() {
        return resendAvailableAt;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public boolean isExpired() {
        return !expiredAt.isAfter(LocalDateTime.now());
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
