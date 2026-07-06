package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.command.RejectProductCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.RejectProductInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RejectProductUseCase implements RejectProductInputPort {

    private final ProductService productService;

    @Override
    public ProductResult execute(RejectProductCommand command) {
        return productService.reject(command);
    }
}
