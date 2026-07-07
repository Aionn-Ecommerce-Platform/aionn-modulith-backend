package com.aionn.catalog.application.port.out.search;

import com.aionn.catalog.domain.model.Product;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;

import java.util.List;

public interface ProductSearchIndexPort {

    void index(Product product);

    void delete(String productId);

    List<String> searchIds(String keyword, OffsetPagination pagination);

    long countMatches(String keyword);
}
