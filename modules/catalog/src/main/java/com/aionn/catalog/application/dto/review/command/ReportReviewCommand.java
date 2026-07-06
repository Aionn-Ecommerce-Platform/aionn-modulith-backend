package com.aionn.catalog.application.dto.review.command;

import com.aionn.sharedkernel.application.command.Command;

public record ReportReviewCommand(String ownerId, String reviewId, String reason) implements Command {
}
