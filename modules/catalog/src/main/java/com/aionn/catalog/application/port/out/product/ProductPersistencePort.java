package com.aionn.catalog.application.port.out.product;

import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductPersistencePort {

    Product save(Product product);

    Optional<Product> findById(String productId);

    List<Product> findAllByIds(Collection<String> productIds);

    List<Product> listByMerchant(String merchantId, OffsetPagination pagination);

    long countByMerchant(String merchantId);

    List<Product> listByStatus(ProductStatus status, OffsetPagination pagination);

    long countByStatus(ProductStatus status);

    List<Product> findAllBySkuIds(Collection<String> skuIds);

    boolean existsByBrandIdAndStatus(String brandId, ProductStatus status);

    boolean existsByCategoryId(String categoryId);

    List<Product> findRelatedProducts(String productId, String brandId, List<String> categoryIds, int limit);

    List<Product> findPopularProducts(int limit);

    List<Product> findPersonalizedProducts(List<String> categoryIds, List<String> brandIds, int limit);

    List<Product> findPublished(int limit, int offset);

    long countPublished();

    List<Product> searchPublished(String query, int limit, int offset);

    long countSearchPublished(String query);

    List<Product> findByIdsPreserveOrder(List<String> productIds);
}
