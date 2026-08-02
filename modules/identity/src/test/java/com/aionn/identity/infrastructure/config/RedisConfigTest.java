package com.aionn.identity.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.SerializationException;

class RedisConfigTest {

    @Test
    void redisValueSerializerRejectsTypesOutsideAllowlistedPackages() {
        var serializer = RedisConfig.redisValueSerializer();
        byte[] payload = serializer.serialize(new File("not-allowed"));

        assertNotNull(payload);
        assertThrows(SerializationException.class, () -> serializer.deserialize(payload));
    }
}
