package com.aionn.payment.adapter.rest.dto.payout.request;

import jakarta.validation.constraints.NotBlank;

public record PayoutCompleteBody(@NotBlank String externalRef) {
}
