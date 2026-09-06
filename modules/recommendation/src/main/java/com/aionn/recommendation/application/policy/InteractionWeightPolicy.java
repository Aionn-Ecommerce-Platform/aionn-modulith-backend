package com.aionn.recommendation.application.policy;

import com.aionn.recommendation.domain.valueobject.InteractionType;
import com.aionn.recommendation.domain.valueobject.InteractionWeight;

/**
 * Supplies the configured weight and half-life for each interaction type. Implemented in
 * infrastructure so application code never reads {@code @ConfigurationProperties} directly.
 */
public interface InteractionWeightPolicy {

    InteractionWeight weightFor(InteractionType type);
}
