package com.aionn.catalog.application.port.in.review;

import com.aionn.catalog.application.dto.review.command.HideReviewCommand;
import com.aionn.catalog.application.dto.review.result.ReviewResult;

public interface HideReviewInputPort {
    ReviewResult execute(HideReviewCommand command);
}
