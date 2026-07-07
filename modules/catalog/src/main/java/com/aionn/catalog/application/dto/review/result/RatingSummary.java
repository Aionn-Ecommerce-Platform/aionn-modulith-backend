package com.aionn.catalog.application.dto.review.result;

import java.util.Map;

public record RatingSummary(
        String productId,
        double average,
        long total,
        Map<Integer, Long> distribution) {
}
