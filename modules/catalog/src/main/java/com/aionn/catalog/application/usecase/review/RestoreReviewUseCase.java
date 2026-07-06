package com.aionn.catalog.application.usecase.review;

import com.aionn.catalog.application.dto.review.command.RestoreReviewCommand;
import com.aionn.catalog.application.dto.review.result.ReviewResult;
import com.aionn.catalog.application.port.in.review.RestoreReviewInputPort;
import com.aionn.catalog.application.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RestoreReviewUseCase implements RestoreReviewInputPort {
    private final ReviewService reviewService;

    @Override
    public ReviewResult execute(RestoreReviewCommand command) {
        return reviewService.restore(command);
    }
}
