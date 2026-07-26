package com.aionn.notification.adapter.rest.dto.subscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDeviceTokenRequest(
        @NotBlank @Size(max = 512) String deviceToken,
        @Size(max = 20) String os) {
}

