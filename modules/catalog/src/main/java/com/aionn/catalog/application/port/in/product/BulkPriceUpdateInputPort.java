package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.command.BulkPriceUpdateCommand;
import com.aionn.catalog.application.dto.product.result.BulkPriceUpdateResult;

public interface BulkPriceUpdateInputPort {
    BulkPriceUpdateResult execute(BulkPriceUpdateCommand command);
}
