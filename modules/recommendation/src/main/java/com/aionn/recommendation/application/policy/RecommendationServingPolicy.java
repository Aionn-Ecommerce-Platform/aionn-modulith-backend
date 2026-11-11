package com.aionn.recommendation.application.policy;

import java.math.BigDecimal;

/**
 * Serving-time limits and shaping thresholds. Kept separate from ranking weights because these
 * values bound how much work a request may trigger rather than how results are scored.
 */
public interface RecommendationServingPolicy {

    /** How far back to look for a user's recent interaction seeds. */
    int seedLookbackDays();

    /** Maximum number of recent interactions used as seeds for collaborative scoring. */
    int seedLimit();

    /** Largest page any surface will build; also the size of a cached slate. */
    int maxPageSize();

    /** Cap on the share of a slate that may be occupied by flash-sale products. */
    BigDecimal maxFlashSaleShare();

    /** Extra candidates fetched for category fallback so the seed product can be excluded. */
    int categoryCandidateOvershoot();
}
