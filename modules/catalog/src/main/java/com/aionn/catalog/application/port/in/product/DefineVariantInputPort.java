package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.command.DefineVariantCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;

public interface DefineVariantInputPort {
    ProductResult execute(DefineVariantCommand command);
}
