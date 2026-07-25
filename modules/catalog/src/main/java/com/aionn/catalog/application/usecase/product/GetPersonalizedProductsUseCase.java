package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.query.GetPersonalizedProductsQuery;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.mapper.ProductResultMapper;
import com.aionn.catalog.application.port.in.product.GetPersonalizedProductsInputPort;
import com.aionn.catalog.application.service.ProductService;
import com.aionn.catalog.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetPersonalizedProductsUseCase implements GetPersonalizedProductsInputPort {

    private final ProductService productService;
    private final ProductResultMapper productResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProductResult> execute(GetPersonalizedProductsQuery query) {
        List<Product> products = productService.getPersonalizedProducts(
                query.userId(),
                query.categoryIds(),
                query.brandIds(),
                query.limit());
        return productResultMapper.toResults(products);
    }
}
