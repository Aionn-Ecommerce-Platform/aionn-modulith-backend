package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.command.PublishProductCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.PublishProductInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublishProductUseCase implements PublishProductInputPort {

    private final ProductService productService;

    @Override
    public ProductResult execute(PublishProductCommand command) {
        return productService.publish(command);
    }
}
