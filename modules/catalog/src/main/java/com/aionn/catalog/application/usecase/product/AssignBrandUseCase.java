package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.command.AssignBrandCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.AssignBrandInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssignBrandUseCase implements AssignBrandInputPort {

    private final ProductService productService;

    @Override
    public ProductResult execute(AssignBrandCommand command) {
        return productService.assignBrand(command);
    }
}
