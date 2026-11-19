package com.aionn.promotion.infrastructure.persistence.repository;

import com.aionn.promotion.domain.valueobject.VoucherType;
import com.aionn.promotion.infrastructure.persistence.entity.VoucherEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface VoucherRepository extends JpaRepository<VoucherEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM VoucherEntity v WHERE v.voucherCode = :code")
    Optional<VoucherEntity> findForUpdate(@Param("code") String voucherCode);

    List<VoucherEntity> findByCampaignId(String campaignId, Pageable pageable);

    List<VoucherEntity> findByMerchantIdOrderByCreatedAtDesc(String merchantId, Pageable pageable);

    @Query("""
            SELECT v FROM VoucherEntity v
             WHERE v.campaignId = :campaignId
               AND (:type IS NULL OR v.type = :type)
             ORDER BY v.createdAt DESC
            """)
    List<VoucherEntity> findByCampaignIdAndType(@Param("campaignId") String campaignId,
            @Param("type") VoucherType type,
            Pageable pageable);

    @Query("""
            SELECT v FROM VoucherEntity v
             WHERE v.merchantId = :merchantId
               AND (:type IS NULL OR v.type = :type)
             ORDER BY v.createdAt DESC
            """)
    List<VoucherEntity> findByMerchantIdAndType(@Param("merchantId") String merchantId,
            @Param("type") VoucherType type,
            Pageable pageable);

    @Query("""
            SELECT v FROM VoucherEntity v
             WHERE v.campaignId = :campaignId
               AND (:type IS NULL OR v.type = :type)
               AND (v.validFrom IS NULL OR v.validFrom <= :now)
               AND (v.validUntil IS NULL OR v.validUntil > :now)
               AND (v.usedCount + v.reservedCount) < v.usageLimit
             ORDER BY v.createdAt DESC
            """)
    List<VoucherEntity> findAvailableByCampaignId(@Param("campaignId") String campaignId,
            @Param("type") VoucherType type,
            @Param("now") Instant now,
            Pageable pageable);

    @Query("""
            SELECT v FROM VoucherEntity v
             WHERE v.merchantId = :merchantId
               AND (:type IS NULL OR v.type = :type)
               AND (v.validFrom IS NULL OR v.validFrom <= :now)
               AND (v.validUntil IS NULL OR v.validUntil > :now)
               AND (v.usedCount + v.reservedCount) < v.usageLimit
             ORDER BY v.createdAt DESC
            """)
    List<VoucherEntity> findAvailableByMerchantId(@Param("merchantId") String merchantId,
            @Param("type") VoucherType type,
            @Param("now") Instant now,
            Pageable pageable);

    @Query("""
            SELECT v.type, v.scope, COUNT(v)
              FROM VoucherEntity v
             WHERE (v.campaignId IN (
                     SELECT c.campaignId FROM PromotionCampaignEntity c
                      WHERE c.status = 'RUNNING'
                        AND c.startDate <= :now
                        AND c.endDate > :now)
                    AND (v.validFrom IS NULL OR v.validFrom <= :now)
                    AND (v.validUntil IS NULL OR v.validUntil > :now)
                    AND (v.usedCount + v.reservedCount) < v.usageLimit)
                OR (v.scope = com.aionn.promotion.domain.valueobject.VoucherScope.SHOP
                    AND (v.validFrom IS NULL OR v.validFrom <= :now)
                    AND (v.validUntil IS NULL OR v.validUntil > :now)
                    AND (v.usedCount + v.reservedCount) < v.usageLimit)
             GROUP BY v.type, v.scope
            """)
    List<Object[]> countAvailable(@Param("now") Instant now);

    @Query("""
        SELECT v.voucherCode AS voucherCode,
               v.campaignId AS campaignId,
               v.usageLimit AS usageLimit,
               v.usedCount AS usedCount,
               v.discountAmount AS discountAmount
          FROM VoucherEntity v
         WHERE v.merchantId = :merchantId
        """)
    List<MerchantVoucherProjection> findMerchantVoucherRows(@Param("merchantId") String merchantId);

    interface MerchantVoucherProjection {
        String getVoucherCode();

        String getCampaignId();

        Integer getUsageLimit();

        Integer getUsedCount();

        BigDecimal getDiscountAmount();
    }
}

