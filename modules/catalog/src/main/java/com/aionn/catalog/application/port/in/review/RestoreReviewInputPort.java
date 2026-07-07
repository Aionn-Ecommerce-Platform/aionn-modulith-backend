package com.aionn.catalog.application.port.in.review;

import com.aionn.catalog.application.dto.review.command.RestoreReviewCommand;
import com.aionn.catalog.application.dto.review.result.ReviewResult;

public interface RestoreReviewInputPort {
    ReviewResult execute(RestoreReviewCommand command);
}
