package com.aionn.catalog.application.dto.review.command;

import com.aionn.sharedkernel.application.command.Command;

public record MerchantReplyCommand(String ownerId, String reviewId, String content) implements Command {
}
