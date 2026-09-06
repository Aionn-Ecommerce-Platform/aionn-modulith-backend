package com.aionn.recommendation.application.port.out;

import java.util.Collection;
import java.util.Set;

/**
 * Availability filter for a ranked slate. Wraps the shared inventory port.
 *
 * <p>A recommendation for something nobody can buy is worse than no recommendation, but availability
 * is also the fastest-moving fact involved - which is why it is applied after ranking and outside the
 * cache, never folded into a cached score.
 */
public interface StockAvailabilityQueryPort {

    Set<String> filterAvailableSkus(Collection<String> skuIds);
}
