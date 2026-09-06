package com.aionn.recommendation.application.policy;

/**
 * Thresholds that decide how much personalisation a user's history can support. Implemented in
 * infrastructure over configuration properties.
 */
public interface ColdStartThresholdPolicy {

    /** At or above this many interactions, content-based signals become usable. */
    int contentOnlyThreshold();

    /** At or above this many interactions, collaborative filtering becomes usable. */
    int fullHybridThreshold();
}
