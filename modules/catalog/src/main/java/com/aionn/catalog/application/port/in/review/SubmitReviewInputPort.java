package com.aionn.catalog.application.port.in.review;

import com.aionn.catalog.application.dto.review.command.SubmitReviewCommand;
import com.aionn.catalog.application.dto.review.result.ReviewResult;

public interface SubmitReviewInputPort {
    ReviewResult execute(SubmitReviewCommand command);
}
