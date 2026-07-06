package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.command.DefineVariantCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.DefineVariantInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefineVariantUseCase implements DefineVariantInputPort {

    private final ProductService productService;

    @Override
    public ProductResult execute(DefineVariantCommand command) {
        return productService.defineVariant(command);
    }
}
