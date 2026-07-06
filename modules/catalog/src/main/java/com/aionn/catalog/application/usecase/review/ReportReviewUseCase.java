package com.aionn.catalog.application.usecase.review;

import com.aionn.catalog.application.dto.review.command.ReportReviewCommand;
import com.aionn.catalog.application.dto.review.result.ReviewResult;
import com.aionn.catalog.application.port.in.review.ReportReviewInputPort;
import com.aionn.catalog.application.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportReviewUseCase implements ReportReviewInputPort {
    private final ReviewService reviewService;

    @Override
    public ReviewResult execute(ReportReviewCommand command) {
        return reviewService.report(command);
    }
}
