package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.product.query.ListProductsByStatusQuery;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.ListProductsByStatusInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListProductsByStatusUseCase implements ListProductsByStatusInputPort {

    private final ProductService productService;

    @Override
    public PageResult<ProductResult> execute(ListProductsByStatusQuery query) {
        return productService.listByStatus(query.status(), query.pagination());
    }
}
