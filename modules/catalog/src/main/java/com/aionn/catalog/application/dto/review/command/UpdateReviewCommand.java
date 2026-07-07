package com.aionn.catalog.application.dto.review.command;

import com.aionn.sharedkernel.application.command.Command;

import java.util.List;

public record UpdateReviewCommand(
        String userId,
        String reviewId,
        int rating,
        String title,
        String content,
        List<String> imageUrls) implements Command {
}
