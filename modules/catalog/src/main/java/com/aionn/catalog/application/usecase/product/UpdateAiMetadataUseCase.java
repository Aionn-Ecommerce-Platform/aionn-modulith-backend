package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.command.UpdateAiMetadataCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.UpdateAiMetadataInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateAiMetadataUseCase implements UpdateAiMetadataInputPort {
    private final ProductService productService;

    @Override
    public ProductResult execute(UpdateAiMetadataCommand command) {
        return productService.updateAiMetadata(command);
    }
}
