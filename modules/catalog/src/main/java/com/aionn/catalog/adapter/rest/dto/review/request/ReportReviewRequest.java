package com.aionn.catalog.adapter.rest.dto.review.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportReviewRequest(@NotBlank @Size(max = 2000) String reason) {
}
