package com.aionn.ordering.adapter.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplyVoucherRequest(@NotBlank @Size(max = 50) String voucherCode) {
}

