package com.aionn.identity.infrastructure.config;

import com.aionn.identity.application.dto.geography.result.GeographyResult;
import com.aionn.identity.infrastructure.config.properties.GeographyCacheProperties;
import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCache;
import com.aionn.sharedkernel.infrastructure.cache.factory.TwoTierCacheFactory;
import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCacheProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class GeographyCacheConfig {

        @Bean(name = "identityGeographyCache")
        public TwoTierCache<String, GeographyResult> identityGeographyCache(
                        TwoTierCacheFactory factory,
                        GeographyCacheProperties properties) {
                return factory.create(
                                new TwoTierCacheProperties(
                                                "identity.geography",
                                                Duration.ofSeconds(properties.l1TtlSeconds()),
                                                properties.l1MaxSize(),
                                                Duration.ofSeconds(properties.l2TtlSeconds())),
                                new TypeReference<>() {
                                });
        }
}
