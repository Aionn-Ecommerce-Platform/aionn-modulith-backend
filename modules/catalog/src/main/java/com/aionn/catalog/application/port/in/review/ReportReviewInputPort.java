package com.aionn.catalog.application.port.in.review;

import com.aionn.catalog.application.dto.review.command.ReportReviewCommand;
import com.aionn.catalog.application.dto.review.result.ReviewResult;

public interface ReportReviewInputPort {
    ReviewResult execute(ReportReviewCommand command);
}
