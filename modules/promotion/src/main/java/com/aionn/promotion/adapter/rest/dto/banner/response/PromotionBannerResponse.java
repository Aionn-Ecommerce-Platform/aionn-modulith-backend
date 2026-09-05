package com.aionn.promotion.adapter.rest.dto.banner.response;

public record PromotionBannerResponse(
        String bannerId,
        String title,
        String imageUrl,
        String imagePublicId,
        String linkUrl,
        int displayOrder) {
}
