package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.command.RejectProductCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;

public interface RejectProductInputPort {
    ProductResult execute(RejectProductCommand command);
}
