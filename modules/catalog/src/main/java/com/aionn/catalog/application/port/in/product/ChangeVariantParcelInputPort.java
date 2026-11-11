package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.command.ChangeVariantParcelCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;

public interface ChangeVariantParcelInputPort {
    ProductResult execute(ChangeVariantParcelCommand command);
}
