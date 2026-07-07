package com.aionn.catalog.application.usecase.review;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.review.query.GetReviewsByProductQuery;
import com.aionn.catalog.application.dto.review.result.ReviewResult;
import com.aionn.catalog.application.port.in.review.GetReviewsByProductInputPort;
import com.aionn.catalog.application.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetReviewsByProductUseCase implements GetReviewsByProductInputPort {
    private final ReviewService reviewService;

    @Override
    public PageResult<ReviewResult> execute(GetReviewsByProductQuery query) {
        return reviewService.getByProduct(query.productId(), query.pagination());
    }
}
