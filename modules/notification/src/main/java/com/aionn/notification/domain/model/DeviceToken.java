package com.aionn.notification.domain.model;

import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.notification.domain.event.NotificationEvents;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;

@Getter
public class DeviceToken extends AggregateRoot {

    private final String tokenId;
    private final String userId;
    private final String deviceToken;
    private final String os;
    private boolean active;
    private final Instant registeredAt;
    private Instant updatedAt;

    public DeviceToken(String tokenId, String userId, String deviceToken, String os,
            boolean active, Instant registeredAt, Instant updatedAt) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.deviceToken = deviceToken;
        this.os = os;
        this.active = active;
        this.registeredAt = registeredAt;
        this.updatedAt = updatedAt;
    }

    public static DeviceToken register(String tokenId, String userId, String deviceToken, String os,
            Clock clock) {
        Instant now = clock.instant();
        DeviceToken dt = new DeviceToken(tokenId, userId, deviceToken, os, true, now, now);
        dt.registerEvent(new NotificationEvents.DeviceTokenRegistered(userId, deviceToken, os, now, now));
        return dt;
    }

    public void deactivate(Clock clock) {
        this.active = false;
        this.updatedAt = clock.instant();
    }

    @Override
    protected String aggregateId() {
        return tokenId;
    }
}
