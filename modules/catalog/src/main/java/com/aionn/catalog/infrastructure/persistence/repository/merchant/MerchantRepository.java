package com.aionn.catalog.infrastructure.persistence.repository.merchant;

import com.aionn.catalog.infrastructure.persistence.entity.MerchantEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MerchantRepository extends JpaRepository<MerchantEntity, String> {

  Optional<MerchantEntity> findByOwnerId(String ownerId);

  boolean existsByOwnerId(String ownerId);

  @Query(value = """
      SELECT m.*
        FROM merchants m
       ORDER BY (
                 SELECT COALESCE(SUM(psc.sold_count), 0)
                   FROM products p
                   JOIN product_sold_counters psc ON psc.product_id = p.product_id
                  WHERE p.merchant_id = m.merchant_id
               ) DESC,
               (
                 SELECT COUNT(*)
                   FROM merchant_followers f
                  WHERE f.merchant_id = m.merchant_id
               ) DESC,
               m.name ASC,
               m.merchant_id ASC
      """, countQuery = "SELECT COUNT(*) FROM merchants", nativeQuery = true)
  Page<MerchantEntity> findAllForStorefront(Pageable pageable);

  @Query(value = """
      SELECT m.*
        FROM merchants m
       WHERE (CAST(:searchPattern AS text) IS NULL OR LOWER(m.name) LIKE CAST(:searchPattern AS text))
         AND (CAST(:provinceCode AS text) IS NULL OR m.province_code = CAST(:provinceCode AS text))
       ORDER BY (
                 SELECT COALESCE(SUM(psc.sold_count), 0)
                   FROM products p
                   JOIN product_sold_counters psc ON psc.product_id = p.product_id
                  WHERE p.merchant_id = m.merchant_id
               ) DESC,
               (
                 SELECT COUNT(*)
                   FROM merchant_followers f
                  WHERE f.merchant_id = m.merchant_id
               ) DESC,
               m.name ASC,
               m.merchant_id ASC
      """, countQuery = """
      SELECT COUNT(*)
        FROM merchants m
       WHERE (CAST(:searchPattern AS text) IS NULL OR LOWER(m.name) LIKE CAST(:searchPattern AS text))
         AND (CAST(:provinceCode AS text) IS NULL OR m.province_code = CAST(:provinceCode AS text))
      """, nativeQuery = true)
  Page<MerchantEntity> searchStorefront(
      String searchPattern,
      String provinceCode,
      Pageable pageable);
}
