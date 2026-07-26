package com.aionn.promotion.application.dto.banner.result;

public record PromotionBannerResult(
        String bannerId,
        String title,
        String imageUrl,
        String linkUrl,
        int displayOrder
) {
}
