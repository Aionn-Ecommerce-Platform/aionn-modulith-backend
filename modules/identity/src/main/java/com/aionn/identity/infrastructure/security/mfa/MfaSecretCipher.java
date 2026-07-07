package com.aionn.identity.infrastructure.security.mfa;

import com.aionn.identity.infrastructure.config.properties.MfaProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class MfaSecretCipher {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;
    private static final int SALT_BYTES = 16;
    private static final int AES_KEY_BITS = 256;
    private static final int PBKDF2_ITERATIONS = 120_000;

    private final char[] encryptionKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public MfaSecretCipher(MfaProperties mfaProperties) {
        String configuredKey = mfaProperties.encryptionKey();
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new IllegalStateException(
                    "MFA encryption key is not configured (identity.mfa.encryption-key)");
        }
        this.encryptionKey = configuredKey.toCharArray();
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return plaintext;
        }
        try {
            // Per-message random salt keeps the derived key unpredictable; it is
            // stored alongside the IV so decrypt can re-derive the same key.
            byte[] salt = new byte[SALT_BYTES];
            secureRandom.nextBytes(salt);
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(salt), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[salt.length + iv.length + ciphertext.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(iv, 0, combined, salt.length, iv.length);
            System.arraycopy(ciphertext, 0, combined, salt.length + iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to encrypt MFA secret", ex);
        }
    }

    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) {
            return encrypted;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encrypted);
            if (combined.length <= SALT_BYTES + IV_BYTES) {
                throw new IllegalStateException("Encrypted MFA secret payload is invalid");
            }
            byte[] salt = Arrays.copyOfRange(combined, 0, SALT_BYTES);
            byte[] iv = Arrays.copyOfRange(combined, SALT_BYTES, SALT_BYTES + IV_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(combined, SALT_BYTES + IV_BYTES, combined.length);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(salt), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to decrypt MFA secret", ex);
        }
    }

    private SecretKeySpec deriveKey(byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(encryptionKey, salt, PBKDF2_ITERATIONS, AES_KEY_BITS);
            try {
                byte[] key = SecretKeyFactory.getInstance(KDF_ALGORITHM)
                        .generateSecret(spec)
                        .getEncoded();
                return new SecretKeySpec(key, "AES");
            } finally {
                spec.clearPassword();
            }
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to derive MFA encryption key", ex);
        }
    }
}
