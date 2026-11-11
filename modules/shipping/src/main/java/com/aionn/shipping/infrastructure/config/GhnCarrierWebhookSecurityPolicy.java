package com.aionn.shipping.infrastructure.config;

import com.aionn.shipping.application.policy.CarrierWebhookSecurityPolicy;
import com.aionn.shipping.infrastructure.carrier.config.GhnProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class GhnCarrierWebhookSecurityPolicy implements CarrierWebhookSecurityPolicy {

    private static final Duration MAX_AGE = Duration.ofMinutes(5);

    private final GhnProperties properties;
    private final Clock clock;

    @Override
    public boolean isAuthorized(String providedSecret) {
        String expectedSecret = properties.webhookSecret();
        if (expectedSecret == null || expectedSecret.isBlank()
                || providedSecret == null || providedSecret.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedSecret.getBytes(StandardCharsets.UTF_8),
                providedSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean isAuthorized(String payload, String signature, String timestamp,
            String legacySecret) {
        if (signature == null || signature.isBlank()
                || timestamp == null || timestamp.isBlank()
                || payload == null) {
            return isAuthorized(legacySecret);
        }
        String secret = properties.webhookSecret();
        if (secret == null || secret.isBlank()) {
            return false;
        }
        try {
            Instant signedAt = Instant.ofEpochSecond(Long.parseLong(timestamp));
            if (signedAt.isBefore(clock.instant().minus(MAX_AGE))
                    || signedAt.isAfter(clock.instant().plus(MAX_AGE))) {
                return false;
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
            byte[] actual = hexToBytes(signature);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception ex) {
            return false;
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return out;
    }
}
