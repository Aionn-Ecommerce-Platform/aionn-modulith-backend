package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.command.DeactivateProductCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;

public interface DeactivateProductInputPort {
    ProductResult execute(DeactivateProductCommand command);
}
