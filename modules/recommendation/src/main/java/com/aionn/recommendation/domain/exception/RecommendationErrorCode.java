package com.aionn.recommendation.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecommendationErrorCode {

    PROFILE_NOT_FOUND("REC_001", "User affinity profile not found"),

    INTERACTION_INVALID("REC_101", "Interaction is not valid"),
    INTERACTION_TYPE_UNSUPPORTED("REC_102", "Interaction type is not supported"),

    SIMILARITY_INVALID("REC_201", "Item similarity is not valid"),

    RANKING_WEIGHTS_INVALID("REC_301", "Ranking weights must be non-negative and sum to a positive value"),

    INVALID_ARGUMENT("REC_900", "Invalid argument");

    private final String code;
    private final String defaultMessage;
}
