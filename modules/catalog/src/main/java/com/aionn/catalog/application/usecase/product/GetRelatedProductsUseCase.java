package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.query.GetRelatedProductsQuery;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.GetRelatedProductsInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetRelatedProductsUseCase implements GetRelatedProductsInputPort {

    private final ProductService productService;

    @Override
    public List<ProductResult> execute(GetRelatedProductsQuery query) {
        return productService.getRelatedProducts(query.productId(), query.limit());
    }
}
