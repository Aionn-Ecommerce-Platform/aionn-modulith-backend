package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.command.AssignCollectionsCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;

public interface AssignCollectionsInputPort {
    ProductResult execute(AssignCollectionsCommand command);
}
