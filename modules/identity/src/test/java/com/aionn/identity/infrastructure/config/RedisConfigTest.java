package com.aionn.identity.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aionn.identity.infrastructure.registration.RegistrationSessionDocument;
import java.io.File;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.SerializationException;

class RedisConfigTest {

    @Test
    void redisValueSerializerRoundTripsRegistrationSessionDocument() {
        var serializer = RedisConfig.redisValueSerializer();
        var document = new RegistrationSessionDocument(
                "reg-1", "+84123456789", "123456", 1, 5,
                Instant.parse("2026-08-05T00:00:30Z"), Instant.parse("2026-08-05T00:05:00Z"),
                false, null, null);

        assertThat(serializer.deserialize(serializer.serialize(document))).isEqualTo(document);
    }

    @Test
    void redisValueSerializerRejectsTypesOutsideAllowlistedPackages() {
        var serializer = RedisConfig.redisValueSerializer();
        byte[] payload = serializer.serialize(new File("not-allowed"));

        assertNotNull(payload);
        assertThrows(SerializationException.class, () -> serializer.deserialize(payload));
    }
}
