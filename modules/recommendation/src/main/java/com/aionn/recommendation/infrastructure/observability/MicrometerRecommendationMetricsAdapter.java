package com.aionn.recommendation.infrastructure.observability;

import com.aionn.recommendation.application.port.out.observability.RecommendationMetricsPort;
import com.aionn.recommendation.domain.valueobject.RecommendationSurface;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class MicrometerRecommendationMetricsAdapter implements RecommendationMetricsPort {

    private static final String TAG_SURFACE = "surface";

    private final MeterRegistry registry;

    public MicrometerRecommendationMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void recordCacheHit(RecommendationSurface surface) {
        registry.counter("recommendation.cache.hits", TAG_SURFACE, surface.name().toLowerCase()).increment();
    }

    @Override
    public void recordCacheMiss(RecommendationSurface surface) {
        registry.counter("recommendation.cache.misses", TAG_SURFACE, surface.name().toLowerCase()).increment();
    }

    @Override
    public void recordLatency(RecommendationSurface surface, long durationMillis) {
        Timer.builder("recommendation.surface.latency")
                .tag(TAG_SURFACE, surface.name().toLowerCase())
                .register(registry)
                .record(durationMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordTrendingFallback(RecommendationSurface surface) {
        registry.counter("recommendation.fallback.trending", TAG_SURFACE, surface.name().toLowerCase()).increment();
    }

    @Override
    public void recordSchedulerExecution(String jobName, long durationMillis, boolean success) {
        Timer.builder("recommendation.scheduler.runtime")
                .tag("job", jobName)
                .tag("success", Boolean.toString(success))
                .register(registry)
                .record(durationMillis, TimeUnit.MILLISECONDS);
    }
}
