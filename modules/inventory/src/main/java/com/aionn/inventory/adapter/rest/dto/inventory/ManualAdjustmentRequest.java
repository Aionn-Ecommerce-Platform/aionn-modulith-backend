package com.aionn.inventory.adapter.rest.dto.inventory;

import com.aionn.inventory.domain.valueobject.AdjustmentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ManualAdjustmentRequest(
                @Positive int qty,
                @NotNull AdjustmentType type,
                @Size(max = 500) String reason) {
}
