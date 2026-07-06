package com.aionn.catalog.application.usecase.review;

import com.aionn.catalog.application.dto.review.command.AdminDeleteReviewCommand;
import com.aionn.catalog.application.port.in.review.AdminDeleteReviewInputPort;
import com.aionn.catalog.application.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminDeleteReviewUseCase implements AdminDeleteReviewInputPort {
    private final ReviewService reviewService;

    @Override
    public void execute(AdminDeleteReviewCommand command) {
        reviewService.adminDelete(command);
    }
}
