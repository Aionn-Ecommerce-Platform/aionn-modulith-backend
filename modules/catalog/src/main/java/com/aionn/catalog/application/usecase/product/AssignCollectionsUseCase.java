package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.command.AssignCollectionsCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.AssignCollectionsInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssignCollectionsUseCase implements AssignCollectionsInputPort {
    private final ProductService productService;

    @Override
    public ProductResult execute(AssignCollectionsCommand command) {
        return productService.assignCollections(command);
    }
}
