package com.aionn.notification.domain.model;

import com.aionn.notification.domain.event.NotificationEvents;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.aionn.sharedkernel.testing.fixture.TestClocks.FIXED;

class NotificationTemplateTest {

    @Test
    void create_extractsPlaceholdersAndRecordsEvent() {
        NotificationTemplate t = NotificationTemplate.create("tpl", "evt",
                NotificationChannel.EMAIL, NotificationCategory.SYSTEM, "vi-VN",
                "Hi {{name}}", "Hello {{name}}, code {{code}}", FIXED);
        assertThat(t.getPlaceholders()).containsExactly("name", "code");
        assertThat(t.pullEvents())
                .anyMatch(env -> env.payload() instanceof NotificationEvents.TemplateCreated);
    }

    @Test
    void render_replacesPlaceholders() {
        NotificationTemplate t = NotificationTemplate.create("tpl", "evt",
                NotificationChannel.EMAIL, NotificationCategory.SYSTEM, "vi-VN",
                "Hi {{name}}", "Hello {{name}}, code {{code}}", FIXED);
        NotificationTemplate.Rendered r = t.render(Map.of("name", "Phat", "code", "123"));
        assertThat(r.subject()).isEqualTo("Hi Phat");
        assertThat(r.content()).isEqualTo("Hello Phat, code 123");
    }

    @Test
    void render_missingPlaceholder_throws() {
        NotificationTemplate t = NotificationTemplate.create("tpl", "evt",
                NotificationChannel.EMAIL, NotificationCategory.SYSTEM, "vi-VN",
                null, "Hello {{name}}", FIXED);
        assertThatThrownBy(() -> t.render(Map.of()))
                .isInstanceOf(NotificationException.class);
    }

    @Test
    void update_bumpsVersionAndExtractsNewPlaceholders() {
        NotificationTemplate t = NotificationTemplate.create("tpl", "evt",
                NotificationChannel.EMAIL, NotificationCategory.SYSTEM, "vi-VN",
                "Hi", "Hello {{name}}", FIXED);
        t.pullEvents();
        t.update("Subj", "Hi {{name}} from {{shop}}", FIXED);
        assertThat(t.getVersion()).isEqualTo(2);
        assertThat(t.getPlaceholders()).containsExactly("name", "shop");
        assertThat(t.pullEvents())
                .anyMatch(env -> env.payload() instanceof NotificationEvents.TemplateUpdated);
    }
}
