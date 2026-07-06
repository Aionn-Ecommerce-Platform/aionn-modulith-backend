package com.aionn.catalog.application.port.out.observability;

public interface CatalogMetricsPort {

    void productLifecycle(String transition);

    void merchantLifecycle(String transition);

    void bulkPriceUpdated(int affectedSkuCount);

    void searchReindexed(long durationMillis);
}
