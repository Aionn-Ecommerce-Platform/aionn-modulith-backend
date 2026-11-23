package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.command.UpdateProductContentCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;

public interface UpdateProductContentInputPort {
    ProductResult execute(UpdateProductContentCommand command);
}
