package com.aionn.recommendation.application.port.out.observability;

import com.aionn.recommendation.domain.valueobject.RecommendationSurface;

public interface RecommendationMetricsPort {

    void recordCacheHit(RecommendationSurface surface);

    void recordCacheMiss(RecommendationSurface surface);

    void recordLatency(RecommendationSurface surface, long durationMillis);

    void recordTrendingFallback(RecommendationSurface surface);

    void recordSchedulerExecution(String jobName, long durationMillis, boolean success);
}
