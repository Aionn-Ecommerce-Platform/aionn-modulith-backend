package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.command.CloneProductCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;

public interface CloneProductInputPort {
    ProductResult execute(CloneProductCommand command);
}
