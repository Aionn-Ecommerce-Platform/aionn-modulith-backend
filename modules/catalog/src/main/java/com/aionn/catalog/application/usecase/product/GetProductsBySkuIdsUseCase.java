package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.query.GetProductsBySkuIdsQuery;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.GetProductsBySkuIdsInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetProductsBySkuIdsUseCase implements GetProductsBySkuIdsInputPort {

    private final ProductService productService;

    @Override
    public List<ProductResult> execute(GetProductsBySkuIdsQuery query) {
        return productService.getBySkuIds(query.skuIds());
    }
}
