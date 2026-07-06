package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.command.CloneProductCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.CloneProductInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CloneProductUseCase implements CloneProductInputPort {

    private final ProductService productService;

    @Override
    public ProductResult execute(CloneProductCommand command) {
        return productService.clone(command);
    }
}
