package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.query.GetPersonalizedProductsQuery;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.GetPersonalizedProductsInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetPersonalizedProductsUseCase implements GetPersonalizedProductsInputPort {

    private final ProductService productService;

    @Override
    public List<ProductResult> execute(GetPersonalizedProductsQuery query) {
        return productService.getPersonalizedProducts(
                query.userId(),
                query.categoryIds(),
                query.brandIds(),
                query.limit());
    }
}
