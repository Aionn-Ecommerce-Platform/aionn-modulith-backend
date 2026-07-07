package com.aionn.catalog.infrastructure.persistence.adapter.analytics;

import com.aionn.catalog.application.dto.analytics.result.ProductAnalyticsResult;
import com.aionn.catalog.application.port.out.analytics.ProductAnalyticsQueryPort;
import com.aionn.catalog.infrastructure.persistence.repository.product.ProductRepository;
import com.aionn.catalog.infrastructure.persistence.repository.review.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductAnalyticsAdapter implements ProductAnalyticsQueryPort {

    private final ProductRepository productRepository;
    private final ProductReviewRepository reviewRepository;

    @Override
    public ProductAnalyticsResult getProductAnalytics() {
        long published = 0;
        long draft = 0;
        long pending = 0;
        long archived = 0;
        for (var row : productRepository.countGroupedByStatus()) {
            long cnt = row.getCnt() == null ? 0 : row.getCnt();
            switch (row.getStatus()) {
                case "PUBLISHED" -> published = cnt;
                case "DRAFT" -> draft = cnt;
                case "PENDING_REVIEW" -> pending = cnt;
                case "ARCHIVED" -> archived = cnt;
                default -> {
                }
            }
        }
        long totalReviews = reviewRepository.countAllVisibleReviews();
        Double avg = reviewRepository.getPlatformAverageRating();
        List<ProductAnalyticsResult.CategoryCount> topCategories = productRepository.countPublishedByCategory(10)
                .stream()
                .map(r -> new ProductAnalyticsResult.CategoryCount(
                        r.getCategoryId(), r.getCnt() == null ? 0 : r.getCnt()))
                .toList();
        return new ProductAnalyticsResult(
                published,
                draft,
                pending,
                archived,
                totalReviews,
                avg == null ? 0.0 : avg,
                topCategories);
    }
}
