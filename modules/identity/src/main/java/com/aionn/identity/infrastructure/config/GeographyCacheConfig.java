package com.aionn.identity.infrastructure.config;

import com.aionn.identity.application.dto.geography.result.GeographyResult;
import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCache;
import com.aionn.sharedkernel.infrastructure.cache.factory.TwoTierCacheFactory;
import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCacheProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class GeographyCacheConfig {

        @Bean(name = "identityGeographyCache")
        public TwoTierCache<String, GeographyResult> identityGeographyCache(
                        TwoTierCacheFactory factory,
                        @Value("${identity.cache.geography.l1-ttl-seconds:1800}") long l1TtlSeconds,
                        @Value("${identity.cache.geography.l1-max-size:5000}") long l1MaxSize,
                        @Value("${identity.cache.geography.l2-ttl-seconds:86400}") long l2TtlSeconds) {
                return factory.create(
                                new TwoTierCacheProperties(
                                                "identity.geography",
                                                Duration.ofSeconds(l1TtlSeconds),
                                                l1MaxSize,
                                                Duration.ofSeconds(l2TtlSeconds)),
                                new TypeReference<>() {
                                });
        }
}
