package com.aionn.catalog.application.dto.review.command;

import com.aionn.sharedkernel.application.command.Command;

public record DeleteReviewCommand(String userId, String reviewId) implements Command {
}
