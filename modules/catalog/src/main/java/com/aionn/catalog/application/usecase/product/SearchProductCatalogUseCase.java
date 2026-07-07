package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.search.ProductSearchCriteria;
import com.aionn.catalog.application.dto.search.ProductSearchResult;
import com.aionn.catalog.application.port.in.product.SearchProductCatalogInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchProductCatalogUseCase implements SearchProductCatalogInputPort {

    private final ProductService productService;

    @Override
    public ProductSearchResult execute(ProductSearchCriteria criteria) {
        return productService.searchCatalog(criteria);
    }
}
