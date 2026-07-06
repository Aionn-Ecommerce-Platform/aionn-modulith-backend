package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.command.SubmitForReviewCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;

public interface SubmitForReviewInputPort {
    ProductResult execute(SubmitForReviewCommand command);
}
