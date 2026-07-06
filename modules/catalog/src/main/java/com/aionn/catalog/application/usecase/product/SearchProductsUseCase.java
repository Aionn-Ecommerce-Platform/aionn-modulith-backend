package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.product.query.SearchProductsQuery;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.SearchProductsInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchProductsUseCase implements SearchProductsInputPort {
    private final ProductService productService;

    @Override
    public PageResult<ProductResult> execute(SearchProductsQuery query) {
        return productService.search(query.keyword(), query.pagination());
    }
}
