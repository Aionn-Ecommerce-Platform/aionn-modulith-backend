package com.aionn.catalog.infrastructure.persistence.adapter.product;

import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.catalog.infrastructure.persistence.mapper.ProductDomainMapper;
import com.aionn.catalog.infrastructure.persistence.repository.product.ProductRepository;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductPersistenceAdapter implements ProductPersistencePort {

    private final ProductRepository jpa;
    private final ProductDomainMapper mapper;

    @Override
    public Product save(Product product) {
        var saved = jpa.save(mapper.toEntity(product));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Product> findById(String productId) {
        return jpa.findById(productId).map(mapper::toDomain);
    }

    @Override
    public List<Product> findAllByIds(Collection<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return jpa.findAllById(productIds).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Product> listByMerchant(String merchantId, OffsetPagination pagination) {
        return jpa.findByMerchantId(merchantId,
                PageRequest.of(pagination.page(), pagination.size(),
                        Sort.by("createdAt").descending().and(Sort.by("productId").ascending())))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByMerchant(String merchantId) {
        return jpa.countByMerchantId(merchantId);
    }

    @Override
    public List<Product> listByStatus(ProductStatus status, OffsetPagination pagination) {
        return jpa.findByStatus(status.name(),
                PageRequest.of(pagination.page(), pagination.size(),
                        Sort.by("createdAt").descending().and(Sort.by("productId").ascending())))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByStatus(ProductStatus status) {
        return jpa.countByStatus(status.name());
    }

    @Override
    public List<Product> findAllBySkuIds(Collection<String> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return List.of();
        }
        return jpa.findAllBySkuIdIn(skuIds).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByBrandIdAndStatus(String brandId, ProductStatus status) {
        return jpa.existsByBrandIdAndStatus(brandId, status.name());
    }

    @Override
    public boolean existsByCategoryId(String categoryId) {
        return jpa.existsByCategoryId(categoryId);
    }

    @Override
    public List<Product> findRelatedProducts(String productId, String brandId, List<String> categoryIds, int limit) {
        List<String> safeCategoryIds = (categoryIds == null || categoryIds.isEmpty()) ? List.of("") : categoryIds;
        return jpa.findRelatedProducts(productId, brandId, safeCategoryIds, limit).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Product> findPopularProducts(int limit) {
        return jpa.findPopularProducts(limit).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Product> findPersonalizedProducts(List<String> categoryIds, List<String> brandIds, int limit) {
        List<String> safeCategoryIds = (categoryIds == null || categoryIds.isEmpty()) ? List.of("") : categoryIds;
        List<String> safeBrandIds = (brandIds == null || brandIds.isEmpty()) ? List.of("") : brandIds;
        return jpa.findPersonalizedProducts(safeCategoryIds, safeBrandIds, limit).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Product> findPublished(int limit, int offset) {
        int safeLimit = Math.max(1, limit);
        int safeOffset = Math.max(0, offset);
        int page = safeOffset / safeLimit;
        return jpa.findPublished(PageRequest.of(page, safeLimit)).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countPublished() {
        return jpa.countPublished();
    }

    @Override
    public List<Product> searchPublished(String query, int limit, int offset) {
        String q = (query == null || query.isBlank()) ? null : query.trim();
        return jpa.searchPublished(q, Math.max(1, limit), Math.max(0, offset)).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countSearchPublished(String query) {
        String q = (query == null || query.isBlank()) ? null : query.trim();
        return jpa.countSearchPublished(q);
    }

    @Override
    public List<Product> findByIdsPreserveOrder(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        java.util.Map<String, Product> byId = jpa.findAllById(productIds).stream()
                .map(mapper::toDomain)
                .collect(java.util.stream.Collectors.toMap(Product::getProductId, p -> p, (a, b) -> a));
        return productIds.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
