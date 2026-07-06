package com.aionn.catalog.application.port.in.review;

import com.aionn.catalog.application.dto.review.command.UpdateReviewCommand;
import com.aionn.catalog.application.dto.review.result.ReviewResult;

public interface UpdateReviewInputPort {
    ReviewResult execute(UpdateReviewCommand command);
}
