package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.command.UpdateMediaCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;

public interface UpdateMediaInputPort {
    ProductResult execute(UpdateMediaCommand command);
}
