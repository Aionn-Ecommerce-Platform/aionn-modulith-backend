package com.aionn.notification.application.dto.subscription.result;

import java.time.Instant;

public record DeviceTokenResult(
        String tokenId,
        String userId,
        String deviceToken,
        String os,
        boolean active,
        Instant registeredAt) {
}

