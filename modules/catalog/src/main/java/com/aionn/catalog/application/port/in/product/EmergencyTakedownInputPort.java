package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.command.EmergencyTakedownCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;

public interface EmergencyTakedownInputPort {
    ProductResult execute(EmergencyTakedownCommand command);
}
