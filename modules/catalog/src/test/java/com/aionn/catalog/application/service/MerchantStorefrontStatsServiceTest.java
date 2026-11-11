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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantStorefrontStatsServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final String MERCHANT_ID = "merchant-1";

    @Mock
    private MerchantPersistencePort merchantRepository;
    @Mock
    private ProductPersistencePort productRepository;
    @Mock
    private ProductReviewPersistencePort reviewRepository;
    @Mock
    private MerchantFollowerPersistencePort followerRepository;
    @Mock
    private ObjectProvider<MerchantChatStatsPort> chatStatsProvider;

    @Test
    void reportsTheMerchantAggregates() {
        Merchant merchant = mock(Merchant.class);
        when(merchant.getCreatedAt()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));
        when(reviewRepository.getMerchantRatingStats(MERCHANT_ID))
                .thenReturn(new ProductReviewPersistencePort.MerchantRatingStats(4.5, 12));
        when(productRepository.countByMerchantAndStatus(MERCHANT_ID, ProductStatus.PUBLISHED)).thenReturn(7L);
        when(followerRepository.countByMerchantId(MERCHANT_ID)).thenReturn(30L);
        when(followerRepository.exists(MERCHANT_ID, "user-1")).thenReturn(true);
        MerchantChatStatsPort chatPort = mock(MerchantChatStatsPort.class);
        when(chatPort.responseStats(eq(MERCHANT_ID), any()))
                .thenReturn(new MerchantChatStatsPort.ResponseStats(10, 8, 60L));
        when(chatStatsProvider.getIfAvailable()).thenReturn(chatPort);

        MerchantStorefrontStatsResult result = service().stats(MERCHANT_ID, "user-1");

        assertThat(result.reviewCount()).isEqualTo(12);
        assertThat(result.averageRating()).isEqualTo(4.5);
        assertThat(result.productCount()).isEqualTo(7);
        assertThat(result.followerCount()).isEqualTo(30);
        assertThat(result.conversations()).isEqualTo(10);
        assertThat(result.respondedConversations()).isEqualTo(8);
        assertThat(result.responseRatePercent()).isEqualTo(80);
        assertThat(result.responseTimeBucket()).isEqualTo("MINUTES");
        assertThat(result.following()).isTrue();
    }

    @Test
    void reportsUnknownChatMetricsWhenTheModuleIsAbsent() {
        Merchant merchant = mock(Merchant.class);
        when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));
        when(reviewRepository.getMerchantRatingStats(MERCHANT_ID))
                .thenReturn(new ProductReviewPersistencePort.MerchantRatingStats(0.0, 0));
        when(chatStatsProvider.getIfAvailable()).thenReturn(null);

        MerchantStorefrontStatsResult result = service().stats(MERCHANT_ID, null);

        assertThat(result.conversations()).isZero();
        assertThat(result.responseRatePercent()).isZero();
        assertThat(result.medianFirstResponseSeconds()).isNull();
        assertThat(result.responseTimeBucket()).isEqualTo("UNKNOWN");
        assertThat(result.following()).isFalse();
    }

    @Test
    void bucketsResponseTimesByMagnitude() {
        Merchant merchant = mock(Merchant.class);
        when(merchantRepository.findById(MERCHANT_ID)).thenReturn(Optional.of(merchant));
        when(reviewRepository.getMerchantRatingStats(MERCHANT_ID))
                .thenReturn(new ProductReviewPersistencePort.MerchantRatingStats(0.0, 0));
        MerchantChatStatsPort chatPort = mock(MerchantChatStatsPort.class);
        when(chatStatsProvider.getIfAvailable()).thenReturn(chatPort);

        assertThat(bucketFor(chatPort, 60L)).isEqualTo("MINUTES");
        assertThat(bucketFor(chatPort, 3600L)).isEqualTo("HOURS");
        assertThat(bucketFor(chatPort, 86_401L)).isEqualTo("DAYS");
        assertThat(bucketFor(chatPort, 86_400L * 30)).isEqualTo("WEEKS");
        assertThat(bucketFor(chatPort, null)).isEqualTo("UNKNOWN");
    }

    @Test
    void reportsAMissingMerchant() {
        when(merchantRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().stats("missing", null))
                .isInstanceOfSatisfying(CatalogException.class,
                        ex -> assertThat(ex.getErrorCode())
                                .isEqualTo(CatalogErrorCode.MERCHANT_NOT_FOUND.getCode()));
    }

    private String bucketFor(MerchantChatStatsPort chatPort, Long medianSeconds) {
        when(chatPort.responseStats(eq(MERCHANT_ID), any()))
                .thenReturn(new MerchantChatStatsPort.ResponseStats(1, 1, medianSeconds));
        return service().stats(MERCHANT_ID, null).responseTimeBucket();
    }

    private MerchantStorefrontStatsService service() {
        return new MerchantStorefrontStatsService(merchantRepository, productRepository, reviewRepository,
                followerRepository, chatStatsProvider, Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
