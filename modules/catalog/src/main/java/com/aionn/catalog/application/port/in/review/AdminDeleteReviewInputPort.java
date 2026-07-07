package com.aionn.catalog.application.port.in.review;

import com.aionn.catalog.application.dto.review.command.AdminDeleteReviewCommand;

public interface AdminDeleteReviewInputPort {
    void execute(AdminDeleteReviewCommand command);
}
