package com.aionn.ucp.infrastructure.persistence.repository;

import com.aionn.ucp.infrastructure.persistence.entity.UcpCheckoutSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UcpCheckoutSessionRepository extends JpaRepository<UcpCheckoutSessionEntity, String> {

    /**
     * Atomic compare-and-set: persists the session only when the stored row still
     * carries {@code expectedVersion} and {@code expectedStatus}. Mirrors the
     * in-memory {@code updateIfMatches} contract against a durable store.
     *
     * @return number of rows updated (0 = CAS lost / state changed concurrently)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE UcpCheckoutSessionEntity e
               SET e.userId        = :#{#s.userId},
                   e.cartId        = :#{#s.cartId},
                   e.status        = :#{#s.status},
                   e.currency      = :#{#s.currency},
                   e.items         = :#{#s.items},
                   e.buyer         = :#{#s.buyer},
                   e.context       = :#{#s.context},
                   e.orderId       = :#{#s.orderId},
                   e.updatedAt     = :#{#s.updatedAt},
                   e.expiresAt     = :#{#s.expiresAt},
                   e.priceSnapshot = :#{#s.priceSnapshot},
                   e.payment       = :#{#s.payment},
                   e.fulfillment   = :#{#s.fulfillment},
                   e.voucherCode   = :#{#s.voucherCode},
                   e.version       = :#{#s.version}
             WHERE e.id       = :#{#s.id}
               AND e.version  = :expectedVersion
               AND e.status   = :expectedStatus
            """)
    int updateIfMatches(@Param("s") UcpCheckoutSessionEntity session,
            @Param("expectedVersion") long expectedVersion,
            @Param("expectedStatus") String expectedStatus);

    @Query("""
            SELECT e FROM UcpCheckoutSessionEntity e
             WHERE e.cartId = :cartId
               AND (e.expiresAt IS NULL OR e.expiresAt > :now)
               AND (LOWER(e.status) = 'incomplete' OR LOWER(e.status) = 'ready_for_complete')
             ORDER BY e.createdAt DESC
            """)
    List<UcpCheckoutSessionEntity> findIncompleteByCartId(@Param("cartId") String cartId,
            @Param("now") Instant now);

    @Query("""
            SELECT e FROM UcpCheckoutSessionEntity e
             WHERE e.orderId = :orderId
               AND (e.expiresAt IS NULL OR e.expiresAt > :now)
            """)
    Optional<UcpCheckoutSessionEntity> findByOrderId(@Param("orderId") String orderId,
            @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM UcpCheckoutSessionEntity e WHERE e.expiresAt IS NOT NULL AND e.expiresAt <= :now")
    int deleteExpired(@Param("now") Instant now);
}
