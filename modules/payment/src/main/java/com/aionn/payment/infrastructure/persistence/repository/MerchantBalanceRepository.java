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
                (merchant_id, currency, pending, available, version, created_at, updated_at)
            VALUES (:merchantId, :currency, 0, 0, 0, :now, :now)
            ON CONFLICT (merchant_id, currency) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("merchantId") String merchantId, @Param("currency") String currency,
            @Param("now") Instant now);

    @Query("SELECT b FROM MerchantBalanceEntity b WHERE b.available >= :minAvailable AND b.currency = :currency")
    java.util.List<MerchantBalanceEntity> findEligibleForAutoPayout(
            @Param("minAvailable") java.math.BigDecimal minAvailable,
            @Param("currency") String currency,
            org.springframework.data.domain.Pageable pageable);
}
