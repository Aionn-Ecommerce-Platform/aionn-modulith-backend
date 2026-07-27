package com.aionn.notification.domain.model;

import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTemplateRenderTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static NotificationTemplate template(String subject, String content) {
        return NotificationTemplate.create("tpl-1", "identity.password-changed",
                NotificationChannel.EMAIL, NotificationCategory.SECURITY, "vi-VN",
                subject, content, CLOCK);
    }

    @Test
    void createWithoutClockUsesSystemTime() {
        NotificationTemplate created = NotificationTemplate.create("tpl-2",
                "identity.password-changed", NotificationChannel.EMAIL,
                NotificationCategory.SECURITY, "vi-VN", "Subject", "Hello");

        assertThat(created.getCreatedAt()).isNotNull();
    }

    @Test
    void createDefaultsLocaleWhenNull() {
        NotificationTemplate created = NotificationTemplate.create("tpl-3",
                "identity.password-changed", NotificationChannel.EMAIL,
                NotificationCategory.SECURITY, null, "Subject", "Hello", CLOCK);

        assertThat(created.getLocale()).isEqualTo("vi-VN");
    }

    @Test
    void createRejectsBlankEventType() {
        assertThatThrownBy(() -> NotificationTemplate.create("tpl-4", "  ",
                NotificationChannel.EMAIL, NotificationCategory.SECURITY, "vi-VN",
                "Subject", "Hello", CLOCK))
                .isInstanceOf(NotificationException.class);
    }

    @Test
    void createRejectsBlankContent() {
        assertThatThrownBy(() -> NotificationTemplate.create("tpl-5", "evt",
                NotificationChannel.EMAIL, NotificationCategory.SECURITY, "vi-VN",
                "Subject", "  ", CLOCK))
                .isInstanceOf(NotificationException.class);
    }

    @Test
    void constructorToleratesNullPlaceholders() {
        NotificationTemplate created = new NotificationTemplate("tpl-6", "evt",
                NotificationChannel.EMAIL, NotificationCategory.SECURITY, "vi-VN",
                "Subject", "Hello", null, 1, true, NOW, NOW);

        assertThat(created.getPlaceholders()).isEmpty();
    }

    @Test
    void placeholdersAreDeduplicatedInOrder() {
        NotificationTemplate created = template("Subject", "{{a}} {{b}} {{a}} {{c}}");

        assertThat(created.getPlaceholders()).containsExactly("a", "b", "c");
    }

    @Test
    void renderReplacesPlaceholdersInSubjectAndContent() {
        NotificationTemplate created = template("Hi {{name}}", "Your code is {{code}}, {{name}}");

        NotificationTemplate.Rendered rendered = created.render(Map.of("name", "Tran", "code", "123"));

        assertThat(rendered.subject()).isEqualTo("Hi Tran");
        assertThat(rendered.content()).isEqualTo("Your code is 123, Tran");
    }

    @Test
    void renderKeepsNullSubjectNull() {
        NotificationTemplate created = template(null, "Hello {{name}}");

        assertThat(created.render(Map.of("name", "Tran")).subject()).isNull();
    }

    @Test
    void renderRejectsMissingPlaceholder() {
        NotificationTemplate created = template("Subject", "Hello {{name}}");

        assertThatThrownBy(() -> created.render(Map.of()))
                .isInstanceOf(NotificationException.class);
    }

    @Test
    void renderToleratesTemplateWithoutPlaceholders() {
        NotificationTemplate created = template("Subject", "Plain body");

        assertThat(created.render(Map.of()).content()).isEqualTo("Plain body");
        assertThat(created.getPlaceholders()).isEqualTo(List.of());
    }

    @Test
    void updateWithoutClockUsesSystemTime() {
        NotificationTemplate created = template("Subject", "Hello {{name}}");

        created.update("New", "Hi {{other}}");

        assertThat(created.getVersion()).isEqualTo(2);
        assertThat(created.getPlaceholders()).containsExactly("other");
    }

    @Test
    void updateRejectsBlankContent() {
        NotificationTemplate created = template("Subject", "Hello {{name}}");

        assertThatThrownBy(() -> created.update("New", "  ", CLOCK))
                .isInstanceOf(NotificationException.class);
    }
}
