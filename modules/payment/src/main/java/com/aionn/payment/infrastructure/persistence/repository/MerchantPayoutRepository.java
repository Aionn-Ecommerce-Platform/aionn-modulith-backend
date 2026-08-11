package com.aionn.payment.infrastructure.persistence.repository;

import com.aionn.payment.infrastructure.persistence.entity.MerchantPayoutEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.Instant;

public interface MerchantPayoutRepository extends JpaRepository<MerchantPayoutEntity, String> {

    List<MerchantPayoutEntity> findByMerchantIdOrderByRequestedAtDesc(String merchantId, Pageable pageable);

    List<MerchantPayoutEntity> findByStatusOrderByRequestedAtAsc(String status, Pageable pageable);

    java.util.Optional<MerchantPayoutEntity> findFirstByMerchantIdAndStatusOrderByCompletedAtDesc(
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
        java.math.BigDecimal getAmount();
        Long getCount();
    }

    interface StatusCountProjection {
        String getStatus();
        Long getCount();
    }

    interface DailyPayoutProjection {
        java.time.LocalDate getDate();
        java.math.BigDecimal getAmount();
        Long getCount();
    }
}
