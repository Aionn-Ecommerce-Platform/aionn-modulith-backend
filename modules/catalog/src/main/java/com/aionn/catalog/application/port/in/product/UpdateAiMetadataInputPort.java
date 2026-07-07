package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.command.UpdateAiMetadataCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;

public interface UpdateAiMetadataInputPort {
    ProductResult execute(UpdateAiMetadataCommand command);
}
