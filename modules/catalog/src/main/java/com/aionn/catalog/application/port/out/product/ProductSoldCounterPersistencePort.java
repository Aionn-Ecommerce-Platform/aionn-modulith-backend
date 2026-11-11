package com.aionn.catalog.application.port.out.product;

import java.util.List;
import java.util.Map;

public interface ProductSoldCounterPersistencePort {

    long getSoldCount(String productId);

    Map<String, Long> getSoldCountsByProductIds(List<String> productIds);

    void incrementSoldCount(String productId, long delta);
}
