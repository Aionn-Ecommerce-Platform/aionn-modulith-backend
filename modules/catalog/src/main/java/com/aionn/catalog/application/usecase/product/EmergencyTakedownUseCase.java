package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.command.EmergencyTakedownCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.EmergencyTakedownInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmergencyTakedownUseCase implements EmergencyTakedownInputPort {
    private final ProductService productService;

    @Override
    public ProductResult execute(EmergencyTakedownCommand command) {
        return productService.emergencyTakedown(command);
    }
}
