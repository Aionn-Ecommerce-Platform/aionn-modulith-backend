package com.aionn.recommendation.domain.exception;

import com.aionn.sharedkernel.common.exception.DomainException;

public class RecommendationException extends DomainException {

    public RecommendationException(RecommendationErrorCode code) {
        super("Recommendation", code.getCode(), code.getDefaultMessage());
    }

    public RecommendationException(RecommendationErrorCode code, String message) {
        super("Recommendation", code.getCode(), message);
    }
}
