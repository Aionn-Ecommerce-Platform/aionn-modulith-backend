package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.command.CreateProductCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.CreateProductInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateProductUseCase implements CreateProductInputPort {

    private final ProductService productService;

    @Override
    public ProductResult execute(CreateProductCommand command) {
        return productService.create(command);
    }
}
