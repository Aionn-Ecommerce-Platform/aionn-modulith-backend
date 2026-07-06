package com.aionn.catalog.application.dto.review.command;

import com.aionn.sharedkernel.application.command.Command;

public record RestoreReviewCommand(String adminId, String reviewId) implements Command {
}
