package com.aionn.identity.infrastructure.security.mfa;

import com.aionn.identity.infrastructure.config.properties.MfaProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MfaSecretCipherTest {

    private final MfaSecretCipher cipher = new MfaSecretCipher(
            new MfaProperties("Aionn", "test-key-32-chars-long-enough-12345", 8));

    @Test
    void encryptDecryptRoundTripPreservesPlainText() {
        String plain = "JBSWY3DPEHPK3PXP";
        String encrypted = cipher.encrypt(plain);

        assertNotEquals(plain, encrypted);
        assertEquals(plain, cipher.decrypt(encrypted));
    }

    @Test
    void encryptProducesDifferentCiphertextEachCallDueToRandomIv() {
        String plain = "secret";
        String first = cipher.encrypt(plain);
        String second = cipher.encrypt(plain);

        assertNotEquals(first, second);
        assertEquals(plain, cipher.decrypt(first));
        assertEquals(plain, cipher.decrypt(second));
    }

    @Test
    void encryptReturnsNullOrBlankUnchanged() {
        assertNull(cipher.encrypt(null));
        assertEquals("", cipher.encrypt(""));
        assertEquals("   ", cipher.encrypt("   "));
    }

    @Test
    void decryptRejectsTamperedCipherText() {
        String encrypted = cipher.encrypt("secret");
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "AAAA";

        assertThrows(IllegalStateException.class, () -> cipher.decrypt(tampered));
    }

    @Test
    void decryptRejectsTooShortPayload() {
        assertThrows(IllegalStateException.class, () -> cipher.decrypt("AAAA"));
    }

    @Test
    void differentKeysProduceDifferentCiphertext() {
        var altCipher = new MfaSecretCipher(new MfaProperties("Aionn", "different-key-also-long-enough-67890", 8));
        String plain = "secret";

        String enc1 = cipher.encrypt(plain);
        assertThrows(IllegalStateException.class, () -> altCipher.decrypt(enc1));
    }
}
