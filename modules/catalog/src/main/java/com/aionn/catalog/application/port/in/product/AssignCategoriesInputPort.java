package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.command.AssignCategoriesCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;

public interface AssignCategoriesInputPort {
    ProductResult execute(AssignCategoriesCommand command);
}
