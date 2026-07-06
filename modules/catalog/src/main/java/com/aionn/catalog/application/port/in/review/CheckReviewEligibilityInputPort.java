package com.aionn.catalog.application.port.in.review;

import com.aionn.catalog.application.dto.review.query.CheckReviewEligibilityQuery;
import com.aionn.catalog.application.dto.review.result.ReviewEligibilityResult;

public interface CheckReviewEligibilityInputPort {
    ReviewEligibilityResult execute(CheckReviewEligibilityQuery query);
}
