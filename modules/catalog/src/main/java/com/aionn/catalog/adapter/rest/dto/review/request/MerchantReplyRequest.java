package com.aionn.catalog.adapter.rest.dto.review.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MerchantReplyRequest(@NotBlank @Size(max = 5000) String content) {
}
