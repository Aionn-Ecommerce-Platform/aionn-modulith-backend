package com.aionn.shipping.infrastructure.policy;

import com.aionn.shipping.application.policy.CarrierWebhookSecurityPolicy;
import com.aionn.shipping.infrastructure.carrier.config.GhnProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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
    public boolean isAuthorized(byte[] payload, String signature, String timestamp,
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
            mac.update(timestamp.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) '.');
            byte[] expected = mac.doFinal(payload);
            byte[] actual = HexFormat.of().parseHex(signature);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception ex) {
            return false;
        }
    }
}
