package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.command.RestoreProductCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.RestoreProductInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RestoreProductUseCase implements RestoreProductInputPort {

    private final ProductService productService;

    @Override
    public ProductResult execute(RestoreProductCommand command) {
        return productService.restore(command);
    }
}
