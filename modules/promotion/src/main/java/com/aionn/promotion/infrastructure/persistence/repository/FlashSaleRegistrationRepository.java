package com.aionn.promotion.infrastructure.persistence.repository;

import com.aionn.promotion.infrastructure.persistence.entity.FlashSaleRegistrationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FlashSaleRegistrationRepository extends JpaRepository<FlashSaleRegistrationEntity, String> {

    @Query("""
            SELECT r FROM FlashSaleRegistrationEntity r
              WHERE r.campaignId = :campaignId AND r.skuId = :skuId
                AND r.status IN ('PENDING','APPROVED')
            """)
    Optional<FlashSaleRegistrationEntity> findActiveBySkuAndCampaign(String campaignId, String skuId);

    @Query("""
            SELECT r FROM FlashSaleRegistrationEntity r
              WHERE r.merchantId = :merchantId
                AND (:status IS NULL OR r.status = :status)
              ORDER BY r.submittedAt DESC
            """)
    List<FlashSaleRegistrationEntity> findByMerchant(String merchantId, String status, Pageable pageable);

    @Query("""
            SELECT r FROM FlashSaleRegistrationEntity r
              WHERE r.campaignId = :campaignId
                AND (:status IS NULL OR r.status = :status)
              ORDER BY r.submittedAt DESC
            """)
    List<FlashSaleRegistrationEntity> findByCampaign(String campaignId, String status, Pageable pageable);

    @Query("""
            SELECT r FROM FlashSaleRegistrationEntity r
              WHERE r.status = :status
              ORDER BY r.submittedAt DESC
            """)
    List<FlashSaleRegistrationEntity> findByStatus(String status, Pageable pageable);

    @Query("""
            SELECT r FROM FlashSaleRegistrationEntity r
              JOIN PromotionCampaignEntity c ON c.campaignId = r.campaignId
              WHERE r.status = 'APPROVED'
                AND c.status = 'RUNNING'
                AND r.skuId IN :skuIds
            """)
    List<FlashSaleRegistrationEntity> findApprovedRunningBySkuIds(List<String> skuIds);

    @Query("""
            SELECT r FROM FlashSaleRegistrationEntity r
              JOIN PromotionCampaignEntity c ON c.campaignId = r.campaignId
              WHERE r.status = 'APPROVED'
                AND c.status = 'RUNNING'
                AND r.productId IN :productIds
            """)
    List<FlashSaleRegistrationEntity> findApprovedRunningByProductIds(List<String> productIds);

    @Query("""
            SELECT r FROM FlashSaleRegistrationEntity r
              JOIN PromotionCampaignEntity c ON c.campaignId = r.campaignId
              WHERE r.status = 'APPROVED'
                AND c.status = 'RUNNING'
              ORDER BY c.endDate ASC
            """)
    List<FlashSaleRegistrationEntity> findAllApprovedRunning(Pageable pageable);
}
