package com.aionn.promotion.application.port.out;

import com.aionn.promotion.domain.model.FlashSaleRegistration;
import com.aionn.promotion.domain.valueobject.FlashSaleRegistrationStatus;

import java.util.List;
import java.util.Optional;

public interface FlashSaleRegistrationPersistencePort {

    FlashSaleRegistration save(FlashSaleRegistration registration);

    Optional<FlashSaleRegistration> findById(String registrationId);

    Optional<FlashSaleRegistration> findActiveBySkuAndCampaign(String campaignId, String skuId);

    List<FlashSaleRegistration> findByMerchant(String merchantId, FlashSaleRegistrationStatus status, int limit);

    List<FlashSaleRegistration> findByCampaign(String campaignId, FlashSaleRegistrationStatus status, int limit);

    List<FlashSaleRegistration> findByStatus(FlashSaleRegistrationStatus status, int limit);

    /**
     * Returns approved registrations for any of the given SKU ids — used by catalog
     * read flows to decorate products with their flash-sale price.
     */
    List<FlashSaleRegistration> findApprovedRunningBySkuIds(List<String> skuIds);

    /** Returns approved registrations for SKUs of a single product. */
    List<FlashSaleRegistration> findApprovedRunningByProductIds(List<String> productIds);

    /** Approved registrations whose campaign is currently RUNNING — for storefront listings. */
    List<FlashSaleRegistration> findAllApprovedRunning(int limit);
}
