package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.command.RemoveVariantCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;

public interface RemoveVariantInputPort {
    ProductResult execute(RemoveVariantCommand command);
}
