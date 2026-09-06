package com.aionn.recommendation.infrastructure.config;

import com.aionn.recommendation.application.dto.result.RecommendationItemResult;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationCacheProperties;
import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCache;
import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCacheProperties;
import com.aionn.sharedkernel.infrastructure.cache.factory.TwoTierCacheFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Caches ranked slates, not availability. Stock is the fastest-changing input and is applied after the
 * cache; folding it in would serve out-of-stock products for the whole TTL.
 */
@Configuration
public class RecommendationCacheConfig {

    @Bean(name = "recommendationHomeFeedCache")
    public TwoTierCache<String, List<RecommendationItemResult>> recommendationHomeFeedCache(
            TwoTierCacheFactory factory, RecommendationCacheProperties properties) {
        return factory.create(
                toCacheProperties("recommendation.home", properties.home()),
                new TypeReference<>() {
                });
    }

    @Bean(name = "recommendationSimilarProductsCache")
    public TwoTierCache<String, List<RecommendationItemResult>> recommendationSimilarProductsCache(
            TwoTierCacheFactory factory, RecommendationCacheProperties properties) {
        return factory.create(
                toCacheProperties("recommendation.similar", properties.similar()),
                new TypeReference<>() {
                });
    }

    @Bean(name = "recommendationTrendingCache")
    public TwoTierCache<String, List<RecommendationItemResult>> recommendationTrendingCache(
            TwoTierCacheFactory factory, RecommendationCacheProperties properties) {
        return factory.create(
                toCacheProperties("recommendation.trending", properties.trending()),
                new TypeReference<>() {
                });
    }

    private static TwoTierCacheProperties toCacheProperties(
            String namespace, RecommendationCacheProperties.Tier tier) {
        return new TwoTierCacheProperties(
                namespace,
                Duration.ofSeconds(tier.l1TtlSeconds()),
                tier.l1MaxSize(),
                Duration.ofSeconds(tier.l2TtlSeconds()));
    }
}
