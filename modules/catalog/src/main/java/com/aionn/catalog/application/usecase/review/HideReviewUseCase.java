package com.aionn.catalog.application.usecase.review;

import com.aionn.catalog.application.dto.review.command.HideReviewCommand;
import com.aionn.catalog.application.dto.review.result.ReviewResult;
import com.aionn.catalog.application.port.in.review.HideReviewInputPort;
import com.aionn.catalog.application.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HideReviewUseCase implements HideReviewInputPort {
    private final ReviewService reviewService;

    @Override
    public ReviewResult execute(HideReviewCommand command) {
        return reviewService.hide(command);
    }
}
