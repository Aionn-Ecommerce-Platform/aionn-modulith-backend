package com.aionn.inventory.adapter.rest.dto.reservation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReleaseReservationRequest(@NotBlank @Size(max = 500) String reason) {
}
