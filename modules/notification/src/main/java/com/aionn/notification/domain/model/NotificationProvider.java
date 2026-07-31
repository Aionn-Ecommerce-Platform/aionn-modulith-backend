package com.aionn.notification.domain.model;

import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.notification.domain.event.NotificationEvents;
import com.aionn.notification.domain.exception.NotificationErrorCode;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class NotificationProvider extends AggregateRoot {

    private final String providerId;
    private final NotificationChannel channel;
    private final String providerType;
    private final Map<String, String> config;
    private boolean active;
    private int rateLimitPerMinute;
    private final String configuredBy;
    private final Instant createdAt;
    private Instant updatedAt;

    public NotificationProvider(String providerId, NotificationChannel channel, String providerType,
            Map<String, String> config, boolean active, int rateLimitPerMinute,
            String configuredBy, Instant createdAt, Instant updatedAt) {
        this.providerId = providerId;
        this.channel = channel;
        this.providerType = providerType;
        this.config = config == null ? new LinkedHashMap<>() : new LinkedHashMap<>(config);
        this.active = active;
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.configuredBy = configuredBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static NotificationProvider configure(String providerId, NotificationChannel channel,
            String providerType, Map<String, String> config, int rateLimitPerMinute, String configuredBy) {
        return configure(providerId, channel, providerType, config, rateLimitPerMinute, configuredBy,
                Clock.systemUTC());
    }

    public static NotificationProvider configure(String providerId, NotificationChannel channel,
            String providerType, Map<String, String> config, int rateLimitPerMinute, String configuredBy,
            Clock clock) {
        if (providerType == null || providerType.isBlank()) {
            throw new NotificationException(NotificationErrorCode.INVALID_ARGUMENT, "providerType required");
        }
        Instant now = clock.instant();
        NotificationProvider p = new NotificationProvider(providerId, channel, providerType, config,
                true, rateLimitPerMinute, configuredBy, now, now);
        p.registerEvent(
                new NotificationEvents.ProviderConfigured(providerId, providerType, true, configuredBy, now, now));
        return p;
    }

    public void update(Map<String, String> config, Integer rateLimitPerMinute, Boolean active,
            String configuredBy) {
        update(config, rateLimitPerMinute, active, configuredBy, Clock.systemUTC());
    }

    public void update(Map<String, String> config, Integer rateLimitPerMinute, Boolean active,
            String configuredBy, Clock clock) {
        if (config != null) {
            this.config.clear();
            this.config.putAll(config);
        }
        if (rateLimitPerMinute != null)
            this.rateLimitPerMinute = rateLimitPerMinute;
        if (active != null)
            this.active = active;
        Instant now = clock.instant();
        this.updatedAt = now;
        registerEvent(new NotificationEvents.ProviderConfigured(providerId, providerType, this.active,
                configuredBy, now, now));
    }

    @Override
    protected String aggregateId() {
        return providerId;
    }
}
