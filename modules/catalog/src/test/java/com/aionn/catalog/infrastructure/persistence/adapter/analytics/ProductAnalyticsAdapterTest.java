package com.aionn.catalog.infrastructure.persistence.adapter.analytics;

import com.aionn.catalog.application.dto.analytics.result.ProductAnalyticsResult;
import com.aionn.catalog.infrastructure.persistence.repository.product.ProductRepository;
import com.aionn.catalog.infrastructure.persistence.repository.review.ProductReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductAnalyticsAdapterTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductReviewRepository reviewRepository;

    @InjectMocks
    private ProductAnalyticsAdapter adapter;

    private static ProductRepository.ProductStatusCount statusCount(String status, Long cnt) {
        return new ProductRepository.ProductStatusCount() {
            @Override
            public String getStatus() {
                return status;
            }

            @Override
            public Long getCnt() {
                return cnt;
            }
        };
    }

    private static ProductRepository.ProductCategoryCount categoryCount(String categoryId, Long cnt) {
        return new ProductRepository.ProductCategoryCount() {
            @Override
            public String getCategoryId() {
                return categoryId;
            }

            @Override
            public Long getCnt() {
                return cnt;
            }
        };
    }

    @Test
    void aggregatesStatusCountsReviewsAndTopCategories() {
        when(productRepository.countGroupedByStatus()).thenReturn(List.of(
                statusCount("PUBLISHED", 10L),
                statusCount("DRAFT", 3L),
                statusCount("PENDING_REVIEW", 2L),
                statusCount("ARCHIVED", 1L),
                statusCount("UNKNOWN", 9L)));
        when(reviewRepository.countAllVisibleReviews()).thenReturn(42L);
        when(reviewRepository.getPlatformAverageRating()).thenReturn(4.5);
        when(productRepository.countPublishedByCategory(10)).thenReturn(List.of(
                categoryCount("cat-a", 5L),
                categoryCount("cat-b", null)));

        ProductAnalyticsResult result = adapter.getProductAnalytics();

        assertThat(result.totalPublished()).isEqualTo(10L);
        assertThat(result.totalDraft()).isEqualTo(3L);
        assertThat(result.totalPendingReview()).isEqualTo(2L);
        assertThat(result.totalArchived()).isEqualTo(1L);
        assertThat(result.totalReviews()).isEqualTo(42L);
        assertThat(result.averageRating()).isEqualTo(4.5);
        assertThat(result.topCategories()).hasSize(2);
        assertThat(result.topCategories().get(1).count()).isZero();
    }

    @Test
    void defaultsAverageRatingToZeroWhenNull() {
        when(productRepository.countGroupedByStatus()).thenReturn(List.of());
        when(reviewRepository.countAllVisibleReviews()).thenReturn(0L);
        when(reviewRepository.getPlatformAverageRating()).thenReturn(null);
        when(productRepository.countPublishedByCategory(10)).thenReturn(List.of());

        ProductAnalyticsResult result = adapter.getProductAnalytics();

        assertThat(result.averageRating()).isZero();
        assertThat(result.totalPublished()).isZero();
    }
}
