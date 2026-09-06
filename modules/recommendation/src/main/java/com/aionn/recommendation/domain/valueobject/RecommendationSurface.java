package com.aionn.recommendation.domain.valueobject;

/**
 * Where a recommendation slate is displayed. Each surface answers a different question, so they use
 * different candidate sources rather than sharing one ranked list.
 */
public enum RecommendationSurface {

    /** Home feed: "what should this person look at?" */
    HOME,

    /** Product detail: "what else is like this?" */
    SIMILAR,

    /** Product detail: "what did other buyers of this also buy?" */
    ALSO_BOUGHT,

    /** Cart: "what completes this basket?" */
    CART
}
