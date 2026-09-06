package com.aionn.recommendation.infrastructure.config;

import com.aionn.recommendation.infrastructure.config.properties.RecommendationCacheProperties;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationColdStartProperties;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationJobProperties;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationRankingProperties;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationSchedulingProperties;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationWeightProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        RecommendationWeightProperties.class,
        RecommendationRankingProperties.class,
        RecommendationColdStartProperties.class,
        RecommendationJobProperties.class,
        RecommendationSchedulingProperties.class,
        RecommendationCacheProperties.class
})
public class RecommendationPropertiesConfig {
}
