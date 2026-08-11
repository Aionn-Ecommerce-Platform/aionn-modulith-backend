package com.aionn.catalog.application.usecase.review;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.review.query.GetMerchantReviewsQuery;
import com.aionn.catalog.application.dto.review.result.ReviewResult;
import com.aionn.catalog.application.mapper.ReviewResultMapper;
import com.aionn.catalog.application.port.in.review.GetMerchantReviewsInputPort;
import com.aionn.catalog.application.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMerchantReviewsUseCase implements GetMerchantReviewsInputPort {
    private final ReviewService reviewService;
    private final ReviewResultMapper reviewResultMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ReviewResult> execute(GetMerchantReviewsQuery query) {
        return reviewResultMapper.toPageResult(
                reviewService.getMerchantReviews(query.ownerId(), query.replied(), query.pagination()));
    }
}
