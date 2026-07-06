package com.aionn.catalog.application.dto.review.command;

import com.aionn.sharedkernel.application.command.Command;

import java.util.List;

public record SubmitReviewCommand(
        String userId,
        String productId,
        int rating,
        String title,
        String content,
        List<String> imageUrls) implements Command {
}
