package com.aionn.recommendation.domain.valueobject;

/**
 * Why a product was recommended. Deliberately an enum rather than free text: the REST layer hands it
 * to clients that localise the wording, so an English sentence produced here could not be
 * translated.
 */
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
