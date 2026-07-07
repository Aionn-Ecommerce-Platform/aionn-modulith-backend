package com.aionn.catalog.application.usecase.review;

import com.aionn.catalog.application.dto.review.query.GetProductRatingSummaryQuery;
import com.aionn.catalog.application.dto.review.result.RatingSummary;
import com.aionn.catalog.application.port.in.review.GetProductRatingSummaryInputPort;
import com.aionn.catalog.application.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProductRatingSummaryUseCase implements GetProductRatingSummaryInputPort {
    private final ReviewService reviewService;

    @Override
    public RatingSummary execute(GetProductRatingSummaryQuery query) {
        return reviewService.getProductRatingSummary(query.productId());
    }
}
