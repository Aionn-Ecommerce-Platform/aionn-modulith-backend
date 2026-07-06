package com.aionn.catalog.infrastructure.search;

import com.aionn.catalog.application.port.out.search.ProductSearchIndexPort;
import com.aionn.catalog.domain.model.Product;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class NoOpProductSearchIndex implements ProductSearchIndexPort {

    @Override
    public void index(Product product) {
    }

    @Override
    public void delete(String productId) {
    }
}
