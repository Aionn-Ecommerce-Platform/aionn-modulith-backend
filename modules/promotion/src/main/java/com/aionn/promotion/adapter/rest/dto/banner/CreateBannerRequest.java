package com.aionn.promotion.adapter.rest.dto.banner;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateBannerRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 500) String imageUrl,
        @NotBlank @Size(max = 500) String linkUrl,
        @PositiveOrZero int displayOrder,
        Boolean active) {
}
