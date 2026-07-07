package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.command.ChangeVariantPriceCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;

public interface ChangeVariantPriceInputPort {
    ProductResult execute(ChangeVariantPriceCommand command);
}
