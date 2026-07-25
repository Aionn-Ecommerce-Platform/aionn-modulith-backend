package com.aionn.catalog.application.usecase.review;

import com.aionn.catalog.application.dto.review.command.UpdateReviewCommand;
import com.aionn.catalog.application.dto.review.result.ReviewResult;
import com.aionn.catalog.application.mapper.ReviewResultMapper;
import com.aionn.catalog.application.port.in.review.UpdateReviewInputPort;
import com.aionn.catalog.application.service.ReviewService;
import com.aionn.catalog.domain.model.ProductReview;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateReviewUseCase implements UpdateReviewInputPort {
    private final ReviewService reviewService;
    private final ReviewResultMapper reviewResultMapper;

    @Override
    @Transactional
    public ReviewResult execute(UpdateReviewCommand command) {
        ProductReview review = reviewService.update(command);
        return reviewResultMapper.toResult(review);
    }
}
