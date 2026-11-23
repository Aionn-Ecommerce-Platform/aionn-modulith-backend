package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.command.WithdrawReviewCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;

public interface WithdrawReviewInputPort {
    ProductResult execute(WithdrawReviewCommand command);
}
