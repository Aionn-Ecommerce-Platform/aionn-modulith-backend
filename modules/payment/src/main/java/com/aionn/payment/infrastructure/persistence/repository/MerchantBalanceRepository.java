package com.aionn.payment.infrastructure.persistence.repository;

import com.aionn.payment.infrastructure.persistence.entity.MerchantBalanceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;

public interface MerchantBalanceRepository
        extends JpaRepository<MerchantBalanceEntity, MerchantBalanceEntity.MerchantBalanceId> {

    @Query("SELECT b FROM MerchantBalanceEntity b WHERE b.merchantId = :merchantId AND b.currency = :currency")
    Optional<MerchantBalanceEntity> findByMerchantAndCurrency(
            @Param("merchantId") String merchantId, @Param("currency") String currency);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM MerchantBalanceEntity b WHERE b.merchantId = :merchantId AND b.currency = :currency")
    Optional<MerchantBalanceEntity> lockByMerchantAndCurrency(
            @Param("merchantId") String merchantId, @Param("currency") String currency);

    @Modifying
    @Query(value = """
            INSERT INTO merchant_balances
                (merchant_id, currency, pending, available, receivable, version, created_at, updated_at)
            VALUES (:merchantId, :currency, 0, 0, 0, 0, :now, :now)
            ON CONFLICT (merchant_id, currency) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("merchantId") String merchantId, @Param("currency") String currency,
            @Param("now") Instant now);

    @Query("SELECT b FROM MerchantBalanceEntity b WHERE b.available >= :minAvailable AND b.currency = :currency")
    java.util.List<MerchantBalanceEntity> findEligibleForAutoPayout(
            @Param("minAvailable") java.math.BigDecimal minAvailable,
            @Param("currency") String currency,
            org.springframework.data.domain.Pageable pageable);

    @Query(value = """
            SELECT b.merchant_id AS merchantId,
                   b.currency AS currency,
                   b.pending AS actualPending,
                   COALESCE(SUM(l.pending_delta), 0) AS ledgerPending,
                   b.available AS actualAvailable,
                   COALESCE(SUM(l.available_delta), 0) AS ledgerAvailable,
                   b.receivable AS actualReceivable,
                   COALESCE(SUM(l.receivable_delta), 0) AS ledgerReceivable
            FROM merchant_balances b
            LEFT JOIN settlement_ledger l
              ON l.merchant_id = b.merchant_id AND l.currency = b.currency
            GROUP BY b.merchant_id, b.currency, b.pending, b.available, b.receivable
            HAVING b.pending <> COALESCE(SUM(l.pending_delta), 0)
                OR b.available <> COALESCE(SUM(l.available_delta), 0)
                OR b.receivable <> COALESCE(SUM(l.receivable_delta), 0)
            """, nativeQuery = true)
    List<ReconciliationMismatchProjection> findReconciliationMismatches();

    interface ReconciliationMismatchProjection {
        String getMerchantId();
        String getCurrency();
        BigDecimal getActualPending();
        BigDecimal getLedgerPending();
        BigDecimal getActualAvailable();
        BigDecimal getLedgerAvailable();
        BigDecimal getActualReceivable();
        BigDecimal getLedgerReceivable();
    }
}
