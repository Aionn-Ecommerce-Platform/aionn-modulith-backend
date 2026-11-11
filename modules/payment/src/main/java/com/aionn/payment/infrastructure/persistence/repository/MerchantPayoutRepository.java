package com.aionn.payment.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.aionn.payment.infrastructure.persistence.entity.MerchantPayoutEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.Instant;

public interface MerchantPayoutRepository extends JpaRepository<MerchantPayoutEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM MerchantPayoutEntity p WHERE p.payoutId = :payoutId")
    Optional<MerchantPayoutEntity> findByIdForUpdate(@Param("payoutId") String payoutId);

    List<MerchantPayoutEntity> findByMerchantIdOrderByRequestedAtDesc(String merchantId, Pageable pageable);

    List<MerchantPayoutEntity> findByStatusOrderByRequestedAtAsc(String status, Pageable pageable);

    Optional<MerchantPayoutEntity> findFirstByMerchantIdAndStatusOrderByCompletedAtDesc(
            String merchantId, String status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) AS amount, COUNT(p) AS count FROM MerchantPayoutEntity p "
            + "WHERE p.status = 'COMPLETED' AND p.completedAt >= :from AND p.completedAt < :to "
            + "AND p.currency = :currency")
    PayoutSummaryProjection summarizeCompleted(@Param("from") Instant from, @Param("to") Instant to,
            @Param("currency") String currency);

    @Query("SELECT p.status AS status, COUNT(p) AS count FROM MerchantPayoutEntity p "
            + "WHERE p.requestedAt >= :from AND p.requestedAt < :to AND p.currency = :currency GROUP BY p.status")
    List<StatusCountProjection> countByStatus(@Param("from") Instant from, @Param("to") Instant to,
            @Param("currency") String currency);

    @Query(value = "SELECT CAST(completed_at AS date) AS date, SUM(amount) AS amount, COUNT(*) AS count "
            + "FROM merchant_payouts WHERE status = 'COMPLETED' AND completed_at >= :from "
            + "AND completed_at < :to AND currency = :currency GROUP BY CAST(completed_at AS date) "
            + "ORDER BY CAST(completed_at AS date)", nativeQuery = true)
    List<DailyPayoutProjection> completedTrend(@Param("from") Instant from, @Param("to") Instant to,
            @Param("currency") String currency);

    interface PayoutSummaryProjection {
        BigDecimal getAmount();
        Long getCount();
    }

    interface StatusCountProjection {
        String getStatus();
        Long getCount();
    }

    interface DailyPayoutProjection {
        LocalDate getDate();
        BigDecimal getAmount();
        Long getCount();
    }
}
