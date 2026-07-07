package com.aionn.catalog.application.usecase.review;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.review.query.GetReportedReviewsQuery;
import com.aionn.catalog.application.dto.review.result.ReviewResult;
import com.aionn.catalog.application.port.in.review.GetReportedReviewsInputPort;
import com.aionn.catalog.application.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetReportedReviewsUseCase implements GetReportedReviewsInputPort {
    private final ReviewService reviewService;

    @Override
    public PageResult<ReviewResult> execute(GetReportedReviewsQuery query) {
        return reviewService.getReportedReviews(query.pagination());
    }
}
