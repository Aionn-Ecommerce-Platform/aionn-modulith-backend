package com.aionn.inventory.adapter.rest.dto.inventory;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TrackBatchAndExpiryRequest(
                @Size(max = 100) String batchNo,
                LocalDate expiryDate) {
}
