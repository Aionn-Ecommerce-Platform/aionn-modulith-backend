package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.command.BulkPriceUpdateCommand;
import com.aionn.catalog.application.dto.product.result.BulkPriceUpdateResult;
import com.aionn.catalog.application.port.in.product.BulkPriceUpdateInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BulkPriceUpdateUseCase implements BulkPriceUpdateInputPort {

    private final ProductService productService;

    @Override
    public BulkPriceUpdateResult execute(BulkPriceUpdateCommand command) {
        return productService.bulkPriceUpdate(command);
    }
}
