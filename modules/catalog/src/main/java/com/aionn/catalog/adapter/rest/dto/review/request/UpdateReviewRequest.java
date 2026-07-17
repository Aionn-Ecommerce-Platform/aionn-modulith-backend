package com.aionn.catalog.adapter.rest.dto.review.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateReviewRequest(
        @Min(1) @Max(5) int rating,
        @Size(max = 200) String title,
        @Size(max = 5000) String content,
        List<@jakarta.validation.constraints.NotBlank String> imageUrls) {
}
