package com.aionn.ordering.infrastructure.persistence.repository;

import java.math.BigDecimal;

import com.aionn.ordering.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {

  @Override
  @EntityGraph(attributePaths = "items")
  Optional<OrderEntity> findById(String orderId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @EntityGraph(attributePaths = "items")
  @Query("SELECT o FROM OrderEntity o WHERE o.orderId = :orderId")
  Optional<OrderEntity> findByIdForUpdate(String orderId);

  @EntityGraph(attributePaths = "items")
  Optional<OrderEntity> findByOrderSn(String orderSn);

  @EntityGraph(attributePaths = "items")
  @Query("SELECT o FROM OrderEntity o WHERE o.orderId = :idOrSn OR o.orderSn = :idOrSn")
  Optional<OrderEntity> findByIdOrOrderSn(String idOrSn);

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

  @Query("""
      SELECT o.orderId FROM OrderEntity o
        WHERE o.status = 'PREPARING'
          AND o.updatedAt <= :cutoff
      ORDER BY o.updatedAt ASC
      """)
  List<String> findPreparingOrderIdsUpdatedBefore(Instant cutoff, Pageable pageable);

  boolean existsByMerchantIdAndStatusNotIn(String merchantId, Collection<String> terminalStatuses);

  boolean existsByUserIdAndStatusNotIn(String userId, Collection<String> terminalStatuses);

  @EntityGraph(attributePaths = "items")
  @Query("""
      SELECT DISTINCT o FROM OrderEntity o
        JOIN o.items item
       WHERE o.userId = :userId
         AND item.id.skuId IN :skuIds
         AND o.status NOT IN ('CANCELLED', 'REJECTED')
       ORDER BY o.createdAt DESC
      """)
  List<OrderEntity> findReviewableOrders(String userId, Collection<String> skuIds, Pageable pageable);

  interface OrderAnalyticsProjection {
    String getStatus();

    BigDecimal getTotalAmount();

    String getCurrency();

    Instant getCreatedAt();
  }

  interface PlatformAnalyticsProjection {
    String getMerchantId();

    String getStatus();

    BigDecimal getTotalAmount();

    String getCurrency();

    Instant getCreatedAt();
  }

  interface TopProductProjection {
    String getSkuId();

    Long getUnitsSold();

    BigDecimal getRevenue();
  }
}
