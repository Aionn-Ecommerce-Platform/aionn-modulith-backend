package com.aionn.ordering.infrastructure.persistence.repository;

import com.aionn.ordering.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {

  @Override
  @EntityGraph(attributePaths = "items")
  Optional<OrderEntity> findById(String orderId);

  @EntityGraph(attributePaths = "items")
  List<OrderEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

  @EntityGraph(attributePaths = "items")
  List<OrderEntity> findByUserIdAndStatusInOrderByCreatedAtDesc(String userId, Collection<String> statuses, Pageable pageable);

  @EntityGraph(attributePaths = "items")
  List<OrderEntity> findByMerchantIdOrderByCreatedAtDesc(String merchantId, Pageable pageable);

  @EntityGraph(attributePaths = "items")
  List<OrderEntity> findByMerchantIdAndStatusInOrderByCreatedAtDesc(String merchantId, Collection<String> statuses, Pageable pageable);

  @Query("""
      SELECT o.status AS status,
             o.totalAmount AS totalAmount,
             o.currency AS currency,
             o.createdAt AS createdAt
        FROM OrderEntity o
       WHERE o.merchantId = :merchantId
         AND o.createdAt >= :from
         AND o.createdAt < :to
       ORDER BY o.createdAt ASC
      """)
  List<OrderAnalyticsProjection> findMerchantAnalyticsRows(String merchantId, Instant from, Instant to);

  @Query("""
      SELECT o.merchantId AS merchantId,
             o.status AS status,
             o.totalAmount AS totalAmount,
             o.currency AS currency,
             o.createdAt AS createdAt
        FROM OrderEntity o
       WHERE o.createdAt >= :from
         AND o.createdAt < :to
       ORDER BY o.createdAt ASC
      """)
  List<PlatformAnalyticsProjection> findPlatformAnalyticsRows(Instant from, Instant to);

  @Query("""
      SELECT item.id.skuId AS skuId,
             SUM(item.qty) AS unitsSold,
             SUM(item.unitPrice * item.qty) AS revenue
        FROM OrderEntity o
        JOIN o.items item
       WHERE o.merchantId = :merchantId
         AND o.status = 'COMPLETED'
         AND o.createdAt >= :from
         AND o.createdAt < :to
       GROUP BY item.id.skuId
       ORDER BY SUM(item.unitPrice * item.qty) DESC
      """)
  List<TopProductProjection> findMerchantTopProductRows(String merchantId, Instant from, Instant to);

  /**
   * Auto-cancel sweep returns ids only; the worker re-loads each one in its own
   * tx.
   */
  @Query("""
      SELECT o.orderId FROM OrderEntity o
        WHERE o.status = 'PENDING'
          AND o.createdAt <= :cutoff
      ORDER BY o.createdAt ASC
      """)
  List<String> findPendingOrderIdsOlderThan(Instant cutoff, Pageable pageable);

  boolean existsByMerchantIdAndStatusNotIn(String merchantId, Collection<String> terminalStatuses);

  @Query("""
      SELECT COUNT(o) > 0 FROM OrderEntity o
        JOIN o.items item
        WHERE o.userId = :userId
          AND o.status = 'COMPLETED'
          AND item.id.skuId IN :skuIds
      """)
  boolean existsCompletedPurchaseForSkus(String userId, Collection<String> skuIds);

  @Query(value = """
      SELECT o.order_id FROM orders o
        JOIN order_items item ON o.order_id = item.order_id
        WHERE o.user_id = :userId
          AND o.status = 'COMPLETED'
          AND item.sku_id IN (:skuIds)
        LIMIT 1
      """, nativeQuery = true)
  String findCompletedOrderIdForSkus(String userId, Collection<String> skuIds);

  interface OrderAnalyticsProjection {
    String getStatus();

    java.math.BigDecimal getTotalAmount();

    String getCurrency();

    Instant getCreatedAt();
  }

  interface PlatformAnalyticsProjection {
    String getMerchantId();

    String getStatus();

    java.math.BigDecimal getTotalAmount();

    String getCurrency();

    Instant getCreatedAt();
  }

  interface TopProductProjection {
    String getSkuId();

    Long getUnitsSold();

    java.math.BigDecimal getRevenue();
  }
}
