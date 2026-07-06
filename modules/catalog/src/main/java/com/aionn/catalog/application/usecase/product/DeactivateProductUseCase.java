package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.command.DeactivateProductCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.DeactivateProductInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeactivateProductUseCase implements DeactivateProductInputPort {

    private final ProductService productService;

    @Override
    public ProductResult execute(DeactivateProductCommand command) {
        return productService.deactivate(command);
    }
}
