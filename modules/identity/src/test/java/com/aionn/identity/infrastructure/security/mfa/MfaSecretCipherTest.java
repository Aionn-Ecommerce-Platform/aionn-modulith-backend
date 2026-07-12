package com.aionn.identity.infrastructure.security.mfa;

import com.aionn.identity.infrastructure.config.properties.MfaProperties;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MfaSecretCipherTest {

    private final MfaSecretCipher cipher = new MfaSecretCipher(
            new MfaProperties("Aionn", "test-key-32-chars-long-enough-12345", 8));

    @Test
    void encryptDecryptRoundTripPreservesPlainText() {
        String plain = "JBSWY3DPEHPK3PXP";
        String encrypted = cipher.encrypt(plain);

        assertThat(encrypted).isNotEqualTo(plain);
        assertThat(cipher.decrypt(encrypted)).isEqualTo(plain);
    }

    @Test
    void encryptProducesDifferentCiphertextEachCallDueToRandomIv() {
        String plain = "secret";
        String first = cipher.encrypt(plain);
        String second = cipher.encrypt(plain);

        assertThat(second).isNotEqualTo(first);
        assertThat(cipher.decrypt(first)).isEqualTo(plain);
        assertThat(cipher.decrypt(second)).isEqualTo(plain);
    }

    @Test
    void encryptReturnsNullOrBlankUnchanged() {
        assertThat(cipher.encrypt(null)).isNull();
        assertThat(cipher.encrypt("")).isEqualTo("");
        assertThat(cipher.encrypt("   ")).isEqualTo("   ");
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
