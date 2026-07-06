package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.command.RestoreProductCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;

public interface RestoreProductInputPort {
    ProductResult execute(RestoreProductCommand command);
}
