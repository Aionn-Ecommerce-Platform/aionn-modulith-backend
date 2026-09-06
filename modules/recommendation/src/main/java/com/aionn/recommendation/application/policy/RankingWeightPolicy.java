package com.aionn.recommendation.application.policy;

import java.math.BigDecimal;

/**
 * Ranking weights and candidate-set sizing. Implemented in infrastructure over configuration
 * properties.
 */
public interface RankingWeightPolicy {

    BigDecimal collaborativeWeight();

    BigDecimal contentWeight();

    BigDecimal popularityWeight();

    BigDecimal categoryAffinityWeight();

    BigDecimal brandAffinityWeight();

    BigDecimal priceFitWeight();

    /**
     * How many candidates to gather per requested result. Over-fetching is required because
     * availability filtering happens after ranking: filtering a list of exactly N leaves fewer than
     * N.
     */
    int candidateOverFetchFactor();

    int maxCandidates();
}
