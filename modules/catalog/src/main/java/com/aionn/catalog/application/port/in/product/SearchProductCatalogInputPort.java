package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.search.ProductSearchCriteria;
import com.aionn.catalog.application.dto.search.ProductSearchResult;

public interface SearchProductCatalogInputPort {

    ProductSearchResult execute(ProductSearchCriteria criteria);
}
