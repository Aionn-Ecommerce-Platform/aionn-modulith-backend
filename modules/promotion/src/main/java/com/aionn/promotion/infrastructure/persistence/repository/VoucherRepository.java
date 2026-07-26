package com.aionn.promotion.infrastructure.persistence.repository;

import com.aionn.promotion.infrastructure.persistence.entity.VoucherEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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

