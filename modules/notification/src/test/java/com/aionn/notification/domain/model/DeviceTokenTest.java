package com.aionn.notification.domain.model;

import com.aionn.notification.domain.event.NotificationEvents;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceTokenTest {

    @Test
    void registerCreatesActiveTokenAndEmitsEvent() {
        DeviceToken dt = DeviceToken.register("tok-1", "user-1", "fcm-abc", "ANDROID");

        assertThat(dt.getTokenId()).isEqualTo("tok-1");
        assertThat(dt.getUserId()).isEqualTo("user-1");
        assertThat(dt.getDeviceToken()).isEqualTo("fcm-abc");
        assertThat(dt.getOs()).isEqualTo("ANDROID");
        assertThat(dt.isActive()).isTrue();
        assertThat(dt.getRegisteredAt()).isNotNull();
        assertThat(dt.getUpdatedAt()).isEqualTo(dt.getRegisteredAt());
        assertThat(dt.peekEvents())
                .anyMatch(env -> env.payload() instanceof NotificationEvents.DeviceTokenRegistered);
    }

    @Test
    void deactivateDisablesTokenAndUpdatesTimestamp() {
        DeviceToken dt = DeviceToken.register("tok-2", "user-2", "fcm-xyz", "IOS");
        dt.pullEvents();

        Instant beforeDeactivate = dt.getUpdatedAt();
        sleepBriefly();
        dt.deactivate();

        assertThat(dt.isActive()).isFalse();
        assertThat(dt.getUpdatedAt()).isAfterOrEqualTo(beforeDeactivate);
    }

    @Test
    void deactivateIsCallableTwiceWithoutThrowing() {
        DeviceToken dt = DeviceToken.register("tok-3", "user-3", "fcm-xyz", "WEB");

        dt.deactivate();
        dt.deactivate();

        assertThat(dt.isActive()).isFalse();
    }

    private static void sleepBriefly() {
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
