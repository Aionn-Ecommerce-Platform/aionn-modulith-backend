package com.aionn.identity.infrastructure.security.mfa;

import com.aionn.identity.application.port.out.security.TotpManagerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class DefaultTotpManager implements TotpManagerPort {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final int SECRET_BYTES = 20;
    private static final int CODE_DIGITS = 6;
    private static final int PERIOD_SECONDS = 30;
    private static final int WINDOW_STEPS = 1;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    @Override
    public boolean verifyCode(String secret, String code) {
        if (secret == null || secret.isBlank() || code == null || !code.matches("\\d{6}")) {
            return false;
        }
        long currentCounter = System.currentTimeMillis() / 1000L / PERIOD_SECONDS;
        for (long offset = -WINDOW_STEPS; offset <= WINDOW_STEPS; offset++) {
            if (generateCode(secret, currentCounter + offset).equals(code)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String buildOtpAuthUri(String issuer, String accountName, String secret) {
        String encodedIssuer = urlEncode(issuer);
        String encodedAccount = urlEncode(accountName);
        return "otpauth://totp/" + encodedIssuer + ":" + encodedAccount
                + "?secret=" + secret
                + "&issuer=" + encodedIssuer
                + "&algorithm=SHA1&digits=" + CODE_DIGITS
                + "&period=" + PERIOD_SECONDS;
    }

    private String generateCode(String secret, long counter) {
        try {
            byte[] secretBytes = decodeBase32(secret);
            byte[] counterBytes = new byte[8];
            for (int i = 7; i >= 0; i--) {
                counterBytes[i] = (byte) (counter & 0xFF);
                counter >>= 8;
            }

            // RFC 6238 mandates HMAC-SHA1 for TOTP; HMAC-SHA1 is not affected by SHA-1
            // collision weaknesses and is required for authenticator-app compatibility.
            Mac mac = Mac.getInstance(HMAC_ALGORITHM); // NOSONAR: HMAC-SHA1 is the RFC 6238 TOTP standard
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(counterBytes);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % 1_000_000;
            return String.format("%06d", otp);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to generate TOTP code", ex);
        }
    }

    private static String encodeBase32(byte[] bytes) {
        StringBuilder result = new StringBuilder((bytes.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                result.append(BASE32_ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            result.append(BASE32_ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return result.toString();
    }

    private static byte[] decodeBase32(String value) {
        String normalized = value.replace("=", "").replace(" ", "").toUpperCase();
        byte[] output = new byte[normalized.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;
        for (char c : normalized.toCharArray()) {
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) {
                throw new IllegalArgumentException("Invalid Base32 secret");
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return output;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
