package com.aionn.catalog.application.usecase.review;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.review.query.GetMyReviewsQuery;
import com.aionn.catalog.application.dto.review.result.ReviewResult;
import com.aionn.catalog.application.mapper.ReviewResultMapper;
import com.aionn.catalog.application.port.in.review.GetMyReviewsInputPort;
import com.aionn.catalog.application.service.ReviewService;
import com.aionn.catalog.domain.model.ProductReview;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMyReviewsUseCase implements GetMyReviewsInputPort {
    private final ReviewService reviewService;
    private final ReviewResultMapper reviewResultMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ReviewResult> execute(GetMyReviewsQuery query) {
        PageResult<ProductReview> page = reviewService.getMyReviews(query.userId(), query.pagination());
        return reviewResultMapper.toPageResult(page);
    }
}
