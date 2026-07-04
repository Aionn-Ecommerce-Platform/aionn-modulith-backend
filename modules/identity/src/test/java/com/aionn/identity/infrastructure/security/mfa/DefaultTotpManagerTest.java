package com.aionn.identity.infrastructure.security.mfa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultTotpManagerTest {

    private final DefaultTotpManager manager = new DefaultTotpManager();

    @Test
    void generateSecretReturnsBase32String() {
        String secret = manager.generateSecret();
        assertNotNull(secret);
        assertTrue(secret.matches("[A-Z2-7]+"));
        assertTrue(secret.length() >= 16);
    }

    @Test
    void generateSecretIsRandomAcrossCalls() {
        assertNotEquals(manager.generateSecret(), manager.generateSecret());
    }

    @Test
    void verifyCodeRejectsInvalidFormat() {
        String secret = manager.generateSecret();

        assertFalse(manager.verifyCode(secret, null));
        assertFalse(manager.verifyCode(secret, ""));
        assertFalse(manager.verifyCode(secret, "abcdef"));
        assertFalse(manager.verifyCode(secret, "1234"));
        assertFalse(manager.verifyCode(secret, "12345"));
        assertFalse(manager.verifyCode(secret, "1234567"));
    }

    @Test
    void verifyCodeRejectsBlankSecret() {
        assertFalse(manager.verifyCode(null, "123456"));
        assertFalse(manager.verifyCode("", "123456"));
    }

    @Test
    void buildOtpAuthUriUrlEncodesIssuerAndAccount() {
        String uri = manager.buildOtpAuthUri("Aionn Pro", "alice@example.com", "ABCDEFGH");

        assertTrue(uri.startsWith("otpauth://totp/"));
        assertTrue(uri.contains("Aionn+Pro"));
        assertTrue(uri.contains("alice%40example.com"));
        assertTrue(uri.contains("secret=ABCDEFGH"));
        assertTrue(uri.contains("algorithm=SHA1"));
        assertTrue(uri.contains("digits=6"));
        assertTrue(uri.contains("period=30"));
    }

    @Test
    void verifyCodeRejectsWrongCode() {
        String secret = "JBSWY3DPEHPK3PXP";
        assertFalse(manager.verifyCode(secret, "000000"));
        assertFalse(manager.verifyCode(secret, "999999"));
    }
}
