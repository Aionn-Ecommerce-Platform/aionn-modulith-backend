package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.product.query.SearchProductsQuery;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.mapper.ProductResultMapper;
import com.aionn.catalog.application.port.in.product.SearchProductsInputPort;
import com.aionn.catalog.application.service.ProductService;
import com.aionn.catalog.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchProductsUseCase implements SearchProductsInputPort {
    private final ProductService productService;
    private final ProductResultMapper productResultMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProductResult> execute(SearchProductsQuery query) {
        PageResult<Product> page = productService.search(query.keyword(), query.pagination());
        return productResultMapper.toPageResult(page);
    }
}
