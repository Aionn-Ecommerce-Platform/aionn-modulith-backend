package com.aionn.catalog.infrastructure.persistence.repository.product;

import com.aionn.catalog.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<ProductEntity, String> {

    @EntityGraph(attributePaths = { "variants", "translations" })
    Page<ProductEntity> findByMerchantId(String merchantId, Pageable pageable);

    long countByMerchantId(String merchantId);

    @EntityGraph(attributePaths = { "variants", "translations" })
    Page<ProductEntity> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);

    @EntityGraph(attributePaths = { "variants", "translations" })
    @Query("SELECT DISTINCT p FROM ProductEntity p JOIN p.variants v WHERE v.skuId IN :skuIds")
    List<ProductEntity> findAllBySkuIdIn(Collection<String> skuIds);

    boolean existsByBrandIdAndStatus(String brandId, String status);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM products WHERE category_ids @> to_jsonb(ARRAY[CAST(:categoryId AS text)]))", nativeQuery = true)
    boolean existsByCategoryId(String categoryId);
}
