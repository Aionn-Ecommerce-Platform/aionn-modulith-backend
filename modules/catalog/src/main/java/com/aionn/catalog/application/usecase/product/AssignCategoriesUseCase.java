package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.command.AssignCategoriesCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.AssignCategoriesInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssignCategoriesUseCase implements AssignCategoriesInputPort {

    private final ProductService productService;

    @Override
    public ProductResult execute(AssignCategoriesCommand command) {
        return productService.categorize(command);
    }
}
