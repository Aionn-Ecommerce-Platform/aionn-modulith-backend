package com.aionn.catalog.application.service;

import com.aionn.catalog.application.port.out.merchant.MerchantFollowerPersistencePort;
import com.aionn.catalog.application.port.out.merchant.MerchantPersistencePort;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.model.Merchant;
import com.aionn.catalog.domain.valueobject.MerchantStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
@Transactional
public class MerchantFollowService {

    private final MerchantPersistencePort merchantPersistencePort;
    private final MerchantFollowerPersistencePort merchantFollowerPersistencePort;
    private final Clock clock;

    public void follow(String merchantId, String userId) {
        Merchant merchant = merchantPersistencePort.findById(merchantId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.MERCHANT_NOT_FOUND));
        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            throw new CatalogException(CatalogErrorCode.MERCHANT_INVALID_TRANSITION,
                    "Only active merchants can be followed");
        }
        merchantFollowerPersistencePort.follow(merchantId, userId, clock.instant());
    }

    public void unfollow(String merchantId, String userId) {
        merchantFollowerPersistencePort.unfollow(merchantId, userId);
    }

    @Transactional(readOnly = true)
    public long followerCount(String merchantId) {
        return merchantFollowerPersistencePort.countByMerchantId(merchantId);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(String merchantId, String userId) {
        return userId != null && merchantFollowerPersistencePort.exists(merchantId, userId);
    }
}
