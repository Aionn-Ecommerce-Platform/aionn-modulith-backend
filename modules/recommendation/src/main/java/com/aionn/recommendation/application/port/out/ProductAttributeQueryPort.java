package com.aionn.recommendation.application.port.out;

import com.aionn.recommendation.application.dto.product.ProductAttributes;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProductAttributeQueryPort {

    Optional<ProductAttributes> findByProductId(String productId);

    Map<String, ProductAttributes> findByProductIds(Collection<String> productIds);

    Optional<String> findProductIdBySkuId(String skuId);

    Map<String, String> findProductIdsBySkuIds(Collection<String> skuIds);

    List<ProductAttributes> findCandidatesByCategoriesOrBrands(
            Collection<String> categoryIds, Collection<String> brandIds, int limit);

    List<ProductAttributes> findRecentlyPublished(int limit);
}
