package com.aionn.catalog.application.policy;

public interface CatalogProductPolicy {

    int getBulkPriceUpdateMaxSize();

    long getBestSellerThreshold();

    int getSearchIndexBatchSize();

    String getCloneNameSuffix();
}
