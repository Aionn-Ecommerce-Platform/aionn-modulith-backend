package com.aionn.promotion.adapter.rest.dto.flashsale;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectFlashSaleRequest(
        @NotBlank @Size(max = 500) String reason) {
}
