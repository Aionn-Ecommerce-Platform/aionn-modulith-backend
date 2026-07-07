package com.aionn.catalog.application.usecase.review;

import com.aionn.catalog.application.dto.review.command.SubmitReviewCommand;
import com.aionn.catalog.application.dto.review.result.ReviewResult;
import com.aionn.catalog.application.port.in.review.SubmitReviewInputPort;
import com.aionn.catalog.application.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubmitReviewUseCase implements SubmitReviewInputPort {
    private final ReviewService reviewService;

    @Override
    public ReviewResult execute(SubmitReviewCommand command) {
        return reviewService.submit(command);
    }
}
