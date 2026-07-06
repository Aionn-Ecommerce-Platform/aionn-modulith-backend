package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.query.GetProductQuery;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.GetProductInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProductUseCase implements GetProductInputPort {

    private final ProductService productService;

    @Override
    public ProductResult execute(GetProductQuery query) {
        return productService.get(query.productId());
    }
}
