package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.command.DefineAttributesCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.DefineAttributesInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefineAttributesUseCase implements DefineAttributesInputPort {

    private final ProductService productService;

    @Override
    public ProductResult execute(DefineAttributesCommand command) {
        return productService.defineAttributes(command);
    }
}
