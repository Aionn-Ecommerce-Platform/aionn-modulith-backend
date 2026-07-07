package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.query.GetPopularProductsQuery;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.GetPopularProductsInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetPopularProductsUseCase implements GetPopularProductsInputPort {

    private final ProductService productService;

    @Override
    public List<ProductResult> execute(GetPopularProductsQuery query) {
        return productService.getPopularProducts(query.limit());
    }
}
