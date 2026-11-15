package com.aionn.recommendation.adapter.rest.dto.recommendation.response;

import com.aionn.recommendation.domain.valueobject.RecommendationReason;

import java.math.BigDecimal;
import java.util.List;

public record RecommendationResponse(
                String productId,
                String name,
                String imageUrl,
                BigDecimal priceFrom,
                BigDecimal originalPriceFrom,
                String currency,
                BigDecimal score,
                RecommendationReason reason,
                List<String> categoryIds,
                String brandId,
                Double rating,
                Long reviewCount,
                Long soldCount) {
}
