package com.aionn.catalog.application.port.in.review;

import com.aionn.catalog.application.dto.review.command.DeleteReviewCommand;

public interface DeleteReviewInputPort {
    void execute(DeleteReviewCommand command);
}
