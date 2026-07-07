package com.aionn.catalog.application.port.in.review;

import com.aionn.catalog.application.dto.review.command.MerchantReplyCommand;
import com.aionn.catalog.application.dto.review.result.ReviewResult;

public interface MerchantReplyReviewInputPort {
    ReviewResult execute(MerchantReplyCommand command);
}
