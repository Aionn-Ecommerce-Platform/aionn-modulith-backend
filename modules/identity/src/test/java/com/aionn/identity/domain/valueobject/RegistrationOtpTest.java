package com.aionn.identity.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrationOtpTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    @Test
    void generateProduces6DigitCodeAndComputesTimingsFromClock() {
        RegistrationOtp otp = RegistrationOtp.generate(60, 300, FIXED_CLOCK);

        assertThat(otp.getCode()).hasSize(6).matches("\\d{6}");
        assertThat(otp.getResendAvailableAt()).isEqualTo(FIXED_INSTANT.plusSeconds(60));
        assertThat(otp.getExpiredAt()).isEqualTo(FIXED_INSTANT.plusSeconds(300));
    }

    @Test
    void generateRejectsNegativeResendCooldown() {
        assertThatThrownBy(() -> RegistrationOtp.generate(-1, 300, FIXED_CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generateRejectsNegativeExpiry() {
        assertThatThrownBy(() -> RegistrationOtp.generate(60, -1, FIXED_CLOCK))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isExpiredReturnsFalseBeforeExpiredAt() {
        RegistrationOtp otp = RegistrationOtp.generate(60, 300, FIXED_CLOCK);
        Clock justBeforeExpiry = Clock.fixed(FIXED_INSTANT.plusSeconds(299), ZoneOffset.UTC);

        assertThat(otp.isExpired(justBeforeExpiry)).isFalse();
    }

    @Test
    void isExpiredReturnsTrueAtOrAfterExpiredAt() {
        RegistrationOtp otp = RegistrationOtp.generate(60, 300, FIXED_CLOCK);
        Clock atExpiry = Clock.fixed(FIXED_INSTANT.plusSeconds(300), ZoneOffset.UTC);
        Clock pastExpiry = Clock.fixed(FIXED_INSTANT.plusSeconds(301), ZoneOffset.UTC);

        assertThat(otp.isExpired(atExpiry)).isTrue();
        assertThat(otp.isExpired(pastExpiry)).isTrue();
    }

    @Test
    void toStringMasksTheCode() {
        RegistrationOtp otp = RegistrationOtp.generate(60, 300, FIXED_CLOCK);

        String rendered = otp.toString();

        assertThat(rendered).contains("code=***").doesNotContain(otp.getCode());
    }
}
