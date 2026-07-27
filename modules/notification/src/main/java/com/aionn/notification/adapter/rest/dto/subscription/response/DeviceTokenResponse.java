package com.aionn.notification.adapter.rest.dto.subscription.response;

import java.time.Instant;

public record DeviceTokenResponse(
        String tokenId,
        String userId,
        String deviceToken,
        String os,
        boolean active,
        Instant registeredAt) {
}
