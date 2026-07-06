package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.command.DefineAttributesCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;

public interface DefineAttributesInputPort {
    ProductResult execute(DefineAttributesCommand command);
}
