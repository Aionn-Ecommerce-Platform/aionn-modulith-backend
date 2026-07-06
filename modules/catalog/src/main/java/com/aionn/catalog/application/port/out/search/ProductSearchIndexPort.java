package com.aionn.catalog.application.port.out.search;

import com.aionn.catalog.domain.model.Product;

public interface ProductSearchIndexPort {

    void index(Product product);

    void delete(String productId);
}
