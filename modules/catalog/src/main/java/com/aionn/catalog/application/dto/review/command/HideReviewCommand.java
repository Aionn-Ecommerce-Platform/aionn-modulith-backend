package com.aionn.catalog.application.dto.review.command;

import com.aionn.sharedkernel.application.command.Command;

public record HideReviewCommand(String adminId, String reviewId) implements Command {
}
