package com.aionn.notification.domain.model;

import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationSubscriptionSnapshotTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static NotificationSubscription subscription() {
        return NotificationSubscription.createDefault("user-1", CLOCK);
    }

    private static NotificationCategory optionalCategory() {
        for (NotificationCategory category : NotificationCategory.values()) {
            if (!category.isMandatory()) {
                return category;
            }
        }
        throw new IllegalStateException("expected at least one optional category");
    }

    @Test
    void createDefaultWithoutClockUsesSystemTime() {
        NotificationSubscription subscription = NotificationSubscription.createDefault("user-2", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(subscription.getCreatedAt()).isNotNull();
        assertThat(subscription.getUpdatedAt()).isNotNull();
    }

    @Test
    void constructorToleratesNullSettings() {
        NotificationSubscription subscription = new NotificationSubscription("user-1", null, NOW, NOW);

        assertThat(subscription.rawSettings()).isEmpty();
        assertThat(subscription.isEnabled(optionalCategory(), NotificationChannel.EMAIL)).isTrue();
    }

    @Test
    void snapshotCoversEveryCategoryAndChannel() {
        Map<NotificationCategory, Map<NotificationChannel, Boolean>> snapshot = subscription().snapshot();

        assertThat(snapshot).hasSize(NotificationCategory.values().length);
        assertThat(snapshot.values())
                .allSatisfy(byChannel -> assertThat(byChannel).hasSize(NotificationChannel.values().length));
    }

    @Test
    void rawSettingsIsADefensiveCopy() {
        NotificationSubscription subscription = subscription();

        Map<String, Boolean> raw = subscription.rawSettings();
        raw.clear();

        assertThat(subscription.rawSettings()).isNotEmpty();
    }

    @Test
    void updateWithoutClockUsesSystemTime() {
        NotificationSubscription subscription = subscription();

        subscription.update(optionalCategory(), NotificationChannel.EMAIL, false, java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(subscription.isEnabled(optionalCategory(), NotificationChannel.EMAIL)).isFalse();
        assertThat(subscription.getUpdatedAt()).isNotNull();
    }

    @Test
    void replaceSettingsAppliesOptionalCategories() {
        NotificationSubscription subscription = subscription();
        NotificationCategory optional = optionalCategory();

        Map<NotificationCategory, Map<NotificationChannel, Boolean>> incoming = new EnumMap<>(
                NotificationCategory.class);
        Map<NotificationChannel, Boolean> byChannel = new EnumMap<>(NotificationChannel.class);
        byChannel.put(NotificationChannel.EMAIL, false);
        byChannel.put(NotificationChannel.PUSH, false);
        incoming.put(optional, byChannel);

        subscription.replaceSettings(incoming, CLOCK);

        assertThat(subscription.isEnabled(optional, NotificationChannel.EMAIL)).isFalse();
        assertThat(subscription.isEnabled(optional, NotificationChannel.PUSH)).isFalse();
        assertThat(subscription.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void replaceSettingsIgnoresAttemptToDisableMandatoryCategory() {
        NotificationSubscription subscription = subscription();
        NotificationCategory mandatory = null;
        for (NotificationCategory category : NotificationCategory.values()) {
            if (category.isMandatory()) {
                mandatory = category;
                break;
            }
        }
        assertThat(mandatory).isNotNull();

        Map<NotificationCategory, Map<NotificationChannel, Boolean>> incoming = new EnumMap<>(
                NotificationCategory.class);
        Map<NotificationChannel, Boolean> byChannel = new EnumMap<>(NotificationChannel.class);
        byChannel.put(NotificationChannel.EMAIL, false);
        incoming.put(mandatory, byChannel);

        subscription.replaceSettings(incoming, CLOCK);

        assertThat(subscription.isEnabled(mandatory, NotificationChannel.EMAIL)).isTrue();
    }

    @Test
    void replaceSettingsWithoutClockUsesSystemTime() {
        NotificationSubscription subscription = subscription();

        subscription.replaceSettings(new EnumMap<>(NotificationCategory.class), java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        assertThat(subscription.getUpdatedAt()).isNotNull();
    }
}
