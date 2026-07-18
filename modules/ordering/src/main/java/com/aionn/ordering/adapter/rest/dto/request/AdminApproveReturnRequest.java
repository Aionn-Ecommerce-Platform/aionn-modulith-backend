package com.aionn.ordering.adapter.rest.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdminApproveReturnRequest(
        @DecimalMin("0.0") BigDecimal refundAmount,
        @Size(min = 3, max = 3) String currency,
        @Size(max = 50) String returnWarehouseId) {
}
