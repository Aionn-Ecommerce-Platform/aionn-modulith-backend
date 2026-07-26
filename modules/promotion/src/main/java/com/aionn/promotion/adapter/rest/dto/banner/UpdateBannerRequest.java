package com.aionn.promotion.adapter.rest.dto.banner;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateBannerRequest(
        @Size(max = 150) String title,
        @Size(max = 500) String imageUrl,
        @Size(max = 500) String linkUrl,
        @PositiveOrZero Integer displayOrder,
        Boolean active) {
}
