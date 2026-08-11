package com.aionn.catalog.application.usecase.review;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.review.query.GetAdminProductReviewsQuery;
import com.aionn.catalog.application.dto.review.result.ReviewResult;
import com.aionn.catalog.application.mapper.ReviewResultMapper;
import com.aionn.catalog.application.port.in.review.GetAdminProductReviewsInputPort;
import com.aionn.catalog.application.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetAdminProductReviewsUseCase implements GetAdminProductReviewsInputPort {
    private final ReviewService reviewService;
    private final ReviewResultMapper reviewResultMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ReviewResult> execute(GetAdminProductReviewsQuery query) {
        return reviewResultMapper.toPageResult(
                reviewService.getAdminProductReviews(query.productId(), query.pagination()));
    }
}
