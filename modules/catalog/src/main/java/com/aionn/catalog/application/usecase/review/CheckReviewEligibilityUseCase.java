package com.aionn.catalog.application.usecase.review;

import com.aionn.catalog.application.dto.review.query.CheckReviewEligibilityQuery;
import com.aionn.catalog.application.dto.review.result.ReviewEligibilityResult;
import com.aionn.catalog.application.port.in.review.CheckReviewEligibilityInputPort;
import com.aionn.catalog.application.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckReviewEligibilityUseCase implements CheckReviewEligibilityInputPort {
    private final ReviewService reviewService;

    @Override
    public ReviewEligibilityResult execute(CheckReviewEligibilityQuery query) {
        return reviewService.checkEligibility(query.userId(), query.productId());
    }
}
