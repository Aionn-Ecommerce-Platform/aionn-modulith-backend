package com.aionn.catalog.application.usecase.review;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.review.query.GetReviewsByProductQuery;
import com.aionn.catalog.application.dto.review.result.ReviewResult;
import com.aionn.catalog.application.mapper.ReviewResultMapper;
import com.aionn.catalog.application.port.in.review.GetReviewsByProductInputPort;
import com.aionn.catalog.application.service.ReviewService;
import com.aionn.catalog.domain.model.ProductReview;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetReviewsByProductUseCase implements GetReviewsByProductInputPort {
    private final ReviewService reviewService;
    private final ReviewResultMapper reviewResultMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ReviewResult> execute(GetReviewsByProductQuery query) {
        PageResult<ProductReview> page = reviewService.getByProduct(query.productId(), query.pagination());
        return reviewResultMapper.toPageResult(page);
    }
}
