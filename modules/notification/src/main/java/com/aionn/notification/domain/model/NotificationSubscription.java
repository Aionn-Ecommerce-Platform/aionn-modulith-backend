package com.aionn.notification.domain.model;

import com.aionn.sharedkernel.domain.Guard;
import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.notification.domain.event.NotificationEvents;
import com.aionn.notification.domain.exception.NotificationErrorCode;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class NotificationSubscription extends AggregateRoot {

    private final String userId;
    private final Map<String, Boolean> settings;
    private final Instant createdAt;
    private Instant updatedAt;

    public NotificationSubscription(String userId, Map<String, Boolean> settings,
            Instant createdAt, Instant updatedAt) {
        this.userId = userId;
        this.settings = settings == null ? new LinkedHashMap<>() : new LinkedHashMap<>(settings);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static NotificationSubscription createDefault(String userId, Clock clock) {
        Instant now = clock.instant();
        Map<String, Boolean> settings = new LinkedHashMap<>();
        // Sensible defaults: every category enabled on every channel.
        for (NotificationCategory cat : NotificationCategory.values()) {
            for (NotificationChannel ch : NotificationChannel.values()) {
                settings.put(key(cat, ch), true);
            }
        }
        return new NotificationSubscription(userId, settings, now, now);
    }

    public boolean isEnabled(NotificationCategory category, NotificationChannel channel) {
        if (category.isMandatory())
            return true;
        return settings.getOrDefault(key(category, channel), true);
    }

    public void update(NotificationCategory category, NotificationChannel channel, boolean enabled,
            Clock clock) {
        Guard.require(!category.isMandatory() || enabled,
                () -> new NotificationException(NotificationErrorCode.SUBSCRIPTION_REQUIRED_CHANNEL,
                        "Cannot disable mandatory category " + category));
        settings.put(key(category, channel), enabled);
        Instant now = clock.instant();
        this.updatedAt = now;
        registerEvent(new NotificationEvents.SubscriptionUpdated(
                userId, category.name(), channel.name(), enabled, now));
    }

    public void replaceSettings(Map<NotificationCategory, Map<NotificationChannel, Boolean>> incoming,
            Clock clock) {
        for (var catEntry : incoming.entrySet()) {
            NotificationCategory cat = catEntry.getKey();
            for (var chEntry : catEntry.getValue().entrySet()) {
                if (cat.isMandatory() && !chEntry.getValue())
                    continue;
                settings.put(key(cat, chEntry.getKey()), chEntry.getValue());
            }
        }
        this.updatedAt = clock.instant();
    }

    public Map<NotificationCategory, Map<NotificationChannel, Boolean>> snapshot() {
        Map<NotificationCategory, Map<NotificationChannel, Boolean>> out = new EnumMap<>(NotificationCategory.class);
        for (NotificationCategory cat : NotificationCategory.values()) {
            Map<NotificationChannel, Boolean> ch = new EnumMap<>(NotificationChannel.class);
            for (NotificationChannel c : NotificationChannel.values()) {
                ch.put(c, settings.getOrDefault(key(cat, c), true));
            }
            out.put(cat, ch);
        }
        return out;
    }

    public Map<String, Boolean> rawSettings() {
        return new HashMap<>(settings);
    }

    private static String key(NotificationCategory cat, NotificationChannel ch) {
        return cat.name() + ":" + ch.name();
    }

    @Override
    protected String aggregateId() {
        return userId;
    }
}
