package com.aionn.notification.domain.model;

import com.aionn.notification.domain.event.NotificationEvents;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.aionn.sharedkernel.testing.fixture.TestClocks.FIXED;

class NotificationSubscriptionTest {

    @Test
    void createDefault_allChannelsEnabled() {
        NotificationSubscription s = NotificationSubscription.createDefault("user-1", FIXED);
        assertThat(s.isEnabled(NotificationCategory.PROMOTION, NotificationChannel.EMAIL)).isTrue();
    }

    @Test
    void securityCategoryAlwaysMandatory_evenIfDisabled() {
        NotificationSubscription s = NotificationSubscription.createDefault("user-1", FIXED);
        assertThatThrownBy(() -> s.update(NotificationCategory.SECURITY, NotificationChannel.EMAIL, false, FIXED))
                .isInstanceOf(NotificationException.class);
        assertThat(s.isEnabled(NotificationCategory.SECURITY, NotificationChannel.EMAIL)).isTrue();
    }

    @Test
    void update_optionalCategory_setsValueAndRecordsEvent() {
        NotificationSubscription s = NotificationSubscription.createDefault("user-1", FIXED);
        s.update(NotificationCategory.PROMOTION, NotificationChannel.EMAIL, false, FIXED);
        assertThat(s.isEnabled(NotificationCategory.PROMOTION, NotificationChannel.EMAIL)).isFalse();
        assertThat(s.pullEvents())
                .anyMatch(env -> env.payload() instanceof NotificationEvents.SubscriptionUpdated);
    }
}
