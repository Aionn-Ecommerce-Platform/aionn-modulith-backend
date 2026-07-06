package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.command.RemoveVariantCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.RemoveVariantInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RemoveVariantUseCase implements RemoveVariantInputPort {
    private final ProductService productService;

    @Override
    public ProductResult execute(RemoveVariantCommand command) {
        return productService.removeVariant(command);
    }
}
