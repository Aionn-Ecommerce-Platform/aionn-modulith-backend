package com.aionn.catalog.application.service;

import com.aionn.catalog.application.dto.merchant.result.MerchantStorefrontStatsResult;
import com.aionn.catalog.application.port.out.merchant.MerchantFollowerPersistencePort;
import com.aionn.catalog.application.port.out.merchant.MerchantPersistencePort;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.application.port.out.review.ProductReviewPersistencePort;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.model.Merchant;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.contracts.port.chat.MerchantChatStatsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class MerchantStorefrontStatsService {

    private static final Duration RESPONSE_WINDOW = Duration.ofDays(30);

    private final MerchantPersistencePort merchantPersistencePort;
    private final ProductPersistencePort productPersistencePort;
    private final ProductReviewPersistencePort productReviewPersistencePort;
    private final MerchantFollowerPersistencePort merchantFollowerPersistencePort;
    private final ObjectProvider<MerchantChatStatsPort> chatStatsProvider;
    private final Clock clock;

    @Transactional(readOnly = true)
    public MerchantStorefrontStatsResult stats(String merchantId, String userId) {
        Merchant merchant = merchantPersistencePort.findById(merchantId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.MERCHANT_NOT_FOUND));
        ProductReviewPersistencePort.MerchantRatingStats rating =
                productReviewPersistencePort.getMerchantRatingStats(merchantId);
        long productCount = productPersistencePort.countByMerchantAndStatus(
                merchantId, ProductStatus.PUBLISHED);
        long followerCount = merchantFollowerPersistencePort.countByMerchantId(merchantId);

        MerchantChatStatsPort.ResponseStats chatStats = null;
        MerchantChatStatsPort port = chatStatsProvider.getIfAvailable();
        if (port != null) {
            chatStats = port.responseStats(merchantId, clock.instant().minus(RESPONSE_WINDOW));
        }
        long conversations = chatStats == null ? 0L : chatStats.conversations();
        long responded = chatStats == null ? 0L : chatStats.respondedConversations();
        Long medianSeconds = chatStats == null ? null : chatStats.medianFirstResponseSeconds();
        int responseRate = conversations == 0
                ? 0
                : (int) Math.round(responded * 100.0 / conversations);

        return new MerchantStorefrontStatsResult(
                rating.reviewCount(),
                rating.averageRating(),
                productCount,
                followerCount,
                merchant.getCreatedAt(),
                conversations,
                responded,
                medianSeconds,
                responseRate,
                responseBucket(medianSeconds),
                userId != null && merchantFollowerPersistencePort.exists(merchantId, userId));
    }

    private static String responseBucket(Long seconds) {
        if (seconds == null) {
            return "UNKNOWN";
        }
        if (seconds <= Duration.ofMinutes(30).toSeconds()) {
            return "MINUTES";
        }
        if (seconds <= Duration.ofHours(24).toSeconds()) {
            return "HOURS";
        }
        if (seconds <= Duration.ofDays(7).toSeconds()) {
            return "DAYS";
        }
        return "WEEKS";
    }
}
