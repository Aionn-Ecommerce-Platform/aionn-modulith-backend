package com.aionn.identity.infrastructure.security.mfa;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultTotpManagerTest {

    private final DefaultTotpManager manager = new DefaultTotpManager();

    @Test
    void generateSecretReturnsBase32String() {
        String secret = manager.generateSecret();
        assertThat(secret).isNotNull();
        assertThat(secret).matches("[A-Z2-7]+");
        assertThat(secret).hasSizeGreaterThanOrEqualTo(16);
    }

    @Test
    void generateSecretIsRandomAcrossCalls() {
        assertThat(manager.generateSecret()).isNotEqualTo(manager.generateSecret());
    }

    @Test
    void verifyCodeRejectsInvalidFormat() {
        String secret = manager.generateSecret();

        assertThat(manager.verifyCode(secret, null)).isFalse();
        assertThat(manager.verifyCode(secret, "")).isFalse();
        assertThat(manager.verifyCode(secret, "abcdef")).isFalse();
        assertThat(manager.verifyCode(secret, "1234")).isFalse();
        assertThat(manager.verifyCode(secret, "12345")).isFalse();
        assertThat(manager.verifyCode(secret, "1234567")).isFalse();
    }

    @Test
    void verifyCodeRejectsBlankSecret() {
        assertThat(manager.verifyCode(null, "123456")).isFalse();
        assertThat(manager.verifyCode("", "123456")).isFalse();
    }

    @Test
    void buildOtpAuthUriUrlEncodesIssuerAndAccount() {
        String uri = manager.buildOtpAuthUri("Aionn Pro", "alice@example.com", "ABCDEFGH");

        assertThat(uri).startsWith("otpauth://totp/");
        assertThat(uri).contains("Aionn+Pro");
        assertThat(uri).contains("alice%40example.com");
        assertThat(uri).contains("secret=ABCDEFGH");
        assertThat(uri).contains("algorithm=SHA1");
        assertThat(uri).contains("digits=6");
        assertThat(uri).contains("period=30");
    }

    @Test
    void verifyCodeRejectsWrongCode() {
        String secret = "JBSWY3DPEHPK3PXP";
        assertThat(manager.verifyCode(secret, "000000")).isFalse();
        assertThat(manager.verifyCode(secret, "999999")).isFalse();
    }
}
