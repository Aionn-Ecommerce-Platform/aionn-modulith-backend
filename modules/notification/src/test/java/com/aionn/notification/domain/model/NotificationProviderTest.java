package com.aionn.notification.domain.model;

import com.aionn.notification.domain.event.NotificationEvents;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationProviderTest {

    @Test
    void configure_setsActiveAndEmitsEvent() {
        NotificationProvider p = NotificationProvider.configure("p1",
                NotificationChannel.EMAIL, "smtp", Map.of("host", "smtp.test"), 60, "admin");
        assertThat(p.isActive()).isTrue();
        assertThat(p.getRateLimitPerMinute()).isEqualTo(60);
        assertThat(p.pullEvents())
                .anyMatch(env -> env.payload() instanceof NotificationEvents.ProviderConfigured);
    }

    @Test
    void update_appliesPartialChanges() {
        NotificationProvider p = NotificationProvider.configure("p1",
                NotificationChannel.EMAIL, "smtp", Map.of("host", "smtp.test"), 60, "admin");
        p.pullEvents();
        p.update(Map.of("host", "smtp.new"), 120, false, "admin");
        assertThat(p.isActive()).isFalse();
        assertThat(p.getRateLimitPerMinute()).isEqualTo(120);
        assertThat(p.getConfig()).containsEntry("host", "smtp.new");
        assertThat(p.pullEvents())
                .anyMatch(env -> env.payload() instanceof NotificationEvents.ProviderConfigured);
    }
}
