package com.aionn.catalog.infrastructure.persistence.repository.product;

import com.aionn.catalog.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  @Query(value = """
      SELECT DISTINCT p.* FROM products p
      WHERE p.status = 'PUBLISHED'
        AND p.product_id != :productId
        AND (
          (p.brand_id IS NOT NULL AND :brandId IS NOT NULL AND p.brand_id = :brandId)
          OR EXISTS (
            SELECT 1 FROM jsonb_array_elements_text(p.category_ids) cat
            WHERE cat IN (:categoryIds)
          )
        )
      ORDER BY p.updated_at DESC
      LIMIT :limit
      """, nativeQuery = true)
  List<ProductEntity> findRelatedProducts(
      @Param("productId") String productId,
      @Param("brandId") String brandId,
      @Param("categoryIds") List<String> categoryIds,
      @Param("limit") int limit);

  @Query(value = """
      SELECT p.* FROM products p
      LEFT JOIN (
        SELECT r.product_id, AVG(r.rating) AS avg_rating, COUNT(r.review_id) AS review_count
        FROM product_reviews r
        WHERE r.status = 'VISIBLE'
        GROUP BY r.product_id
      ) rev ON p.product_id = rev.product_id
      WHERE p.status = 'PUBLISHED'
      ORDER BY COALESCE(rev.avg_rating, 0) DESC,
               COALESCE(rev.review_count, 0) DESC,
               p.updated_at DESC
      LIMIT :limit
      """, nativeQuery = true)
  List<ProductEntity> findPopularProducts(@Param("limit") int limit);

  @Query(value = """
      SELECT DISTINCT p.* FROM products p
      WHERE p.status = 'PUBLISHED'
        AND (
          (p.brand_id IS NOT NULL AND p.brand_id IN (:brandIds))
          OR EXISTS (
            SELECT 1 FROM jsonb_array_elements_text(p.category_ids) cat
            WHERE cat IN (:categoryIds)
          )
        )
      ORDER BY p.updated_at DESC
      LIMIT :limit
      """, nativeQuery = true)
  List<ProductEntity> findPersonalizedProducts(
      @Param("categoryIds") List<String> categoryIds,
      @Param("brandIds") List<String> brandIds,
      @Param("limit") int limit);

  @EntityGraph(attributePaths = { "variants", "translations" })
  @Query("""
      SELECT p FROM ProductEntity p
      WHERE p.status = 'PUBLISHED'
      ORDER BY p.updatedAt DESC
      """)
  List<ProductEntity> findPublished(Pageable pageable);

  @Query(value = """
      SELECT * FROM products
      WHERE status = 'PUBLISHED'
      ORDER BY updated_at DESC
      LIMIT :limit OFFSET :offset
      """, nativeQuery = true)
  List<ProductEntity> findPublishedRaw(
      @Param("limit") int limit,
      @Param("offset") int offset);

  @Query(value = "SELECT COUNT(*) FROM products p WHERE p.status = 'PUBLISHED'", nativeQuery = true)
  long countPublished();

  @EntityGraph(attributePaths = { "variants", "translations" })
  @Query(value = """
      SELECT p.* FROM products p
      WHERE p.status = 'PUBLISHED'
        AND (:q IS NULL OR p.name ILIKE CONCAT('%', :q, '%'))
      ORDER BY p.updated_at DESC
      LIMIT :limit OFFSET :offset
      """, nativeQuery = true)
  List<ProductEntity> searchPublished(@Param("q") String q, @Param("limit") int limit, @Param("offset") int offset);

  @Query(value = """
      SELECT COUNT(*) FROM products p
      WHERE p.status = 'PUBLISHED'
        AND (:q IS NULL OR p.name ILIKE CONCAT('%', :q, '%'))
      """, nativeQuery = true)
  long countSearchPublished(@Param("q") String q);

  @Query(value = """
      SELECT p.* FROM products p
      WHERE p.status = :status
        AND (:q IS NULL OR p.name ILIKE CONCAT('%', :q, '%'))
        AND (:merchantId IS NULL OR p.merchant_id = :merchantId)
        AND (:brandIdsCsv = '' OR p.brand_id = ANY(string_to_array(:brandIdsCsv, ',')))
        AND (:categoryIdsCsv = '' OR EXISTS (
          SELECT 1 FROM jsonb_array_elements_text(p.category_ids) category_id
          WHERE category_id = ANY(string_to_array(:categoryIdsCsv, ','))))
        AND ((:priceMin IS NULL AND :priceMax IS NULL) OR EXISTS (
          SELECT 1 FROM product_variants variant
          WHERE variant.product_id = p.product_id
            AND (:priceMin IS NULL OR variant.price >= :priceMin)
            AND (:priceMax IS NULL OR variant.price <= :priceMax)))
      ORDER BY p.updated_at DESC, p.product_id ASC
      LIMIT :limit OFFSET :offset
      """, nativeQuery = true)
  List<ProductEntity> searchFallback(
      @Param("q") String query,
      @Param("merchantId") String merchantId,
      @Param("status") String status,
      @Param("brandIdsCsv") String brandIdsCsv,
      @Param("categoryIdsCsv") String categoryIdsCsv,
      @Param("priceMin") java.math.BigDecimal priceMin,
      @Param("priceMax") java.math.BigDecimal priceMax,
      @Param("limit") int limit,
      @Param("offset") int offset);

  @Query(value = """
      SELECT COUNT(*) FROM products p
      WHERE p.status = :status
        AND (:q IS NULL OR p.name ILIKE CONCAT('%', :q, '%'))
        AND (:merchantId IS NULL OR p.merchant_id = :merchantId)
        AND (:brandIdsCsv = '' OR p.brand_id = ANY(string_to_array(:brandIdsCsv, ',')))
        AND (:categoryIdsCsv = '' OR EXISTS (
          SELECT 1 FROM jsonb_array_elements_text(p.category_ids) category_id
          WHERE category_id = ANY(string_to_array(:categoryIdsCsv, ','))))
        AND ((:priceMin IS NULL AND :priceMax IS NULL) OR EXISTS (
          SELECT 1 FROM product_variants variant
          WHERE variant.product_id = p.product_id
            AND (:priceMin IS NULL OR variant.price >= :priceMin)
            AND (:priceMax IS NULL OR variant.price <= :priceMax)))
      """, nativeQuery = true)
  long countFallback(
      @Param("q") String query,
      @Param("merchantId") String merchantId,
      @Param("status") String status,
      @Param("brandIdsCsv") String brandIdsCsv,
      @Param("categoryIdsCsv") String categoryIdsCsv,
      @Param("priceMin") java.math.BigDecimal priceMin,
      @Param("priceMax") java.math.BigDecimal priceMax);

  @Query("SELECT p.status AS status, COUNT(p) AS cnt FROM ProductEntity p GROUP BY p.status")
  List<ProductStatusCount> countGroupedByStatus();

  @Query(value = """
      SELECT cat AS categoryId, COUNT(*) AS cnt
      FROM products p, jsonb_array_elements_text(p.category_ids) cat
      WHERE p.status = 'PUBLISHED'
      GROUP BY cat
      ORDER BY cnt DESC
      LIMIT :limit
      """, nativeQuery = true)
  List<ProductCategoryCount> countPublishedByCategory(@Param("limit") int limit);

  interface ProductStatusCount {
    String getStatus();

    Long getCnt();
  }

  interface ProductCategoryCount {
    String getCategoryId();

    Long getCnt();
  }
}
