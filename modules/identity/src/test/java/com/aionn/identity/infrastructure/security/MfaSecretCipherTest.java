package com.aionn.identity.infrastructure.security;

import com.aionn.identity.infrastructure.config.properties.MfaProperties;
import com.aionn.identity.infrastructure.security.mfa.MfaSecretCipher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MfaSecretCipherTest {

    private static MfaSecretCipher build(String key) {
        return new MfaSecretCipher(new MfaProperties("Aionn", key, 8));
    }

    @Test
    void encryptedSecretIsReversibleAndDifferentFromPlaintext() {
        MfaSecretCipher cipher = build("test-only-key-please-rotate-in-prod");
        String plaintext = "JBSWY3DPEHPK3PXP";

        String encrypted = cipher.encrypt(plaintext);

        assertNotNull(encrypted);
        assertNotEquals(plaintext, encrypted);
        assertEquals(plaintext, cipher.decrypt(encrypted));
    }

    @Test
    void encryptSamePlaintextTwiceProducesDifferentCiphertexts() {
        MfaSecretCipher cipher = build("test-only-key-please-rotate-in-prod");

        String first = cipher.encrypt("the-same-secret");
        String second = cipher.encrypt("the-same-secret");

        assertNotEquals(first, second);
    }

    @Test
    void blankValuesAreReturnedAsIs() {
        MfaSecretCipher cipher = build("test-only-key-please-rotate-in-prod");

        assertNull(cipher.encrypt(null));
        assertEquals("", cipher.encrypt(""));
        assertNull(cipher.decrypt(null));
    }

    @Test
    void constructorRequiresEncryptionKey() {
        assertThrows(IllegalStateException.class,
                () -> new MfaSecretCipher(new MfaProperties("Aionn", "  ", 8)));
    }
}
