package com.aionn.catalog.application.usecase.review;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.review.query.GetMyReviewsQuery;
import com.aionn.catalog.application.dto.review.result.ReviewResult;
import com.aionn.catalog.application.port.in.review.GetMyReviewsInputPort;
import com.aionn.catalog.application.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetMyReviewsUseCase implements GetMyReviewsInputPort {
    private final ReviewService reviewService;

    @Override
    public PageResult<ReviewResult> execute(GetMyReviewsQuery query) {
        return reviewService.getMyReviews(query.userId(), query.pagination());
    }
}
