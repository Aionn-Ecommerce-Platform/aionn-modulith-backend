package com.aionn.recommendation.domain.valueobject;

/** Why a product was recommended. An enum, not free text, so clients can localise the wording. */
public enum RecommendationReason {

    /** Recent momentum across all users; also the cold-start answer. */
    TRENDING,

    /** Co-occurrence with something the user viewed or bought. */
    SIMILAR_TO_VIEWED,

    /** Co-occurrence within completed orders. */
    FREQUENTLY_BOUGHT_TOGETHER,

    /** Category, brand or price-band match against the user's affinity profile. */
    MATCHES_YOUR_INTERESTS,

    /** Recently published product with no behavioural history yet. */
    NEW_ARRIVAL
}
