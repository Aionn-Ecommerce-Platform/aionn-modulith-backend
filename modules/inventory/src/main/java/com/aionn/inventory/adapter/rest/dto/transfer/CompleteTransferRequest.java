package com.aionn.inventory.adapter.rest.dto.transfer;

import jakarta.validation.constraints.Positive;

public record CompleteTransferRequest(@Positive int receivedQty) {
}
