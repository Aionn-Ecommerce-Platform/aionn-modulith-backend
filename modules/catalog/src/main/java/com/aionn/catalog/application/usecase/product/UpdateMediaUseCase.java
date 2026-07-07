package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.command.UpdateMediaCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.UpdateMediaInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateMediaUseCase implements UpdateMediaInputPort {
    private final ProductService productService;

    @Override
    public ProductResult execute(UpdateMediaCommand command) {
        return productService.updateMedia(command);
    }
}
