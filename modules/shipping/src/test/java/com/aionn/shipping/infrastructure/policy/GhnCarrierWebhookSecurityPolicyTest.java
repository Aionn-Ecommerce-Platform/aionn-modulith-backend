package com.aionn.shipping.infrastructure.policy;

import com.aionn.shipping.infrastructure.carrier.config.GhnProperties;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GhnCarrierWebhookSecurityPolicyTest {

    private static final String PAYLOAD = "{\"trackingCode\":\"TRACK_1\",\"type\":\"DELIVERED\"}";
    private static final String TIMESTAMP = "1767225600";

    private final GhnProperties properties = mock(GhnProperties.class);
    private final Clock clock = Clock.fixed(Instant.ofEpochSecond(Long.parseLong(TIMESTAMP)), ZoneOffset.UTC);
    private final GhnCarrierWebhookSecurityPolicy policy =
            new GhnCarrierWebhookSecurityPolicy(properties, clock);

    @Test
    void rejectsWebhookWhenNoSecretIsConfigured() {
        when(properties.webhookSecret()).thenReturn("  ");

        assertThat(policy.isAuthorized(null)).isFalse();
        assertThat(policy.isAuthorized("any-secret")).isFalse();
        assertThat(policy.isAuthorized(PAYLOAD.getBytes(StandardCharsets.UTF_8), hmac("any-secret"), TIMESTAMP, null)).isFalse();
    }

    @Test
    void requiresExactMatchWhenSecretIsConfigured() {
        when(properties.webhookSecret()).thenReturn("expected-secret");

        assertThat(policy.isAuthorized("expected-secret")).isTrue();
        assertThat(policy.isAuthorized("wrong-secret")).isFalse();
        assertThat(policy.isAuthorized(null)).isFalse();
    }

    @Test
    void acceptsValidHmacSignature() {
        when(properties.webhookSecret()).thenReturn("expected-secret");

        assertThat(policy.isAuthorized(PAYLOAD.getBytes(StandardCharsets.UTF_8), hmac("expected-secret"), TIMESTAMP, null)).isTrue();
        assertThat(policy.isAuthorized(PAYLOAD.getBytes(StandardCharsets.UTF_8), hmac("wrong-secret"), TIMESTAMP, null)).isFalse();
    }

    @Test
    void rejectsStaleHmacTimestamp() {
        when(properties.webhookSecret()).thenReturn("expected-secret");
        String stale = String.valueOf(Long.parseLong(TIMESTAMP) - 600);

        assertThat(policy.isAuthorized(PAYLOAD.getBytes(StandardCharsets.UTF_8), hmacAt("expected-secret", stale), stale, null)).isFalse();
    }

    private static String hmac(String secret) {
        return hmacAt(secret, TIMESTAMP);
    }

    private static String hmacAt(String secret, String timestamp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((timestamp + "." + PAYLOAD).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
