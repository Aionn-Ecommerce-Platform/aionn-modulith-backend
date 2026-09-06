package com.aionn.recommendation.infrastructure.integration.catalog;

import com.aionn.recommendation.application.port.out.ProductAttributeQueryPort;
import com.aionn.sharedkernel.integration.port.catalog.CatalogProductAttributeQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Translates catalog's shared attribute view into the recommendation-local
 * contract, so application
 * code depends on its own port rather than on another module's record shape.
 */
@Component
@RequiredArgsConstructor
public class CatalogProductAttributeAdapter implements ProductAttributeQueryPort {

    private final CatalogProductAttributeQueryPort catalogProductAttributeQuery;

    @Override
    public Optional<ProductAttributes> findByProductId(String productId) {
        return catalogProductAttributeQuery.findByProductId(productId)
                .map(CatalogProductAttributeAdapter::toAttributes);
    }

    @Override
    public Map<String, ProductAttributes> findByProductIds(Collection<String> productIds) {
        Map<String, CatalogProductAttributeQueryPort.ProductAttributeView> views = catalogProductAttributeQuery
                .findByProductIds(productIds);
        Map<String, ProductAttributes> attributes = LinkedHashMap.newLinkedHashMap(views.size());
        views.forEach((productId, view) -> attributes.put(productId, toAttributes(view)));
        return attributes;
    }

    @Override
    public Optional<String> findProductIdBySkuId(String skuId) {
        return catalogProductAttributeQuery.findProductIdBySkuId(skuId);
    }

    @Override
    public Map<String, String> findProductIdsBySkuIds(Collection<String> skuIds) {
        return catalogProductAttributeQuery.findProductIdsBySkuIds(skuIds);
    }

    @Override
    public List<ProductAttributes> findCandidatesByCategoriesOrBrands(
            Collection<String> categoryIds, Collection<String> brandIds, int limit) {
        return toAttributes(catalogProductAttributeQuery.findByCategoriesOrBrands(
                categoryIds, brandIds, limit));
    }

    @Override
    public List<ProductAttributes> findRecentlyPublished(int limit) {
        return toAttributes(catalogProductAttributeQuery.findRecentlyPublished(limit));
    }

    private static List<ProductAttributes> toAttributes(
            List<CatalogProductAttributeQueryPort.ProductAttributeView> views) {
        List<ProductAttributes> attributes = new ArrayList<>(views.size());
        for (CatalogProductAttributeQueryPort.ProductAttributeView view : views) {
            attributes.add(toAttributes(view));
        }
        return attributes;
    }

    private static ProductAttributes toAttributes(
            CatalogProductAttributeQueryPort.ProductAttributeView view) {
        return new ProductAttributes(
                view.productId(),
                view.name(),
                view.brandId(),
                view.categoryIds(),
                view.skuIds(),
                view.imageUrl(),
                view.priceFrom(),
                view.currency());
    }
}
