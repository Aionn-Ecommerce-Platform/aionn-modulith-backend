package com.aionn.catalog.infrastructure.observability;

import com.aionn.catalog.application.port.out.observability.CatalogMetricsPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class MicrometerCatalogMetricsAdapter implements CatalogMetricsPort {

    private final MeterRegistry registry;
    private final Timer reindexTimer;

    public MicrometerCatalogMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
        this.reindexTimer = Timer.builder("catalog.search.reindex")
                .description("Reindex duration for merchant lifecycle changes")
                .register(registry);
    }

    @Override
    public void productLifecycle(String transition) {
        registry.counter("catalog.product.lifecycle", "transition", transition).increment();
    }

    @Override
    public void merchantLifecycle(String transition) {
        registry.counter("catalog.merchant.lifecycle", "transition", transition).increment();
    }

    @Override
    public void bulkPriceUpdated(int affectedSkuCount) {
        registry.counter("catalog.product.bulk_price_update").increment(affectedSkuCount);
    }

    @Override
    public void searchReindexed(long durationMillis) {
        reindexTimer.record(durationMillis, TimeUnit.MILLISECONDS);
    }
}
