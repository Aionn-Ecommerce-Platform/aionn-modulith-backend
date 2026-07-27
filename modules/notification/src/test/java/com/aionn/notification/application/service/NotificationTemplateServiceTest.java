package com.aionn.notification.application.service;

import com.aionn.notification.application.dto.template.command.TemplateCommands;
import com.aionn.notification.application.port.out.NotificationTemplatePersistencePort;
import com.aionn.notification.domain.exception.NotificationErrorCode;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.model.NotificationTemplate;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.sharedkernel.application.port.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationTemplateServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String EVENT = "identity.password-changed";

    @Mock
    private NotificationTemplatePersistencePort repository;
    @Mock
    private EventPublisher eventPublisher;

    private NotificationTemplateService service() {
        return new NotificationTemplateService(repository, eventPublisher, CLOCK);
    }

    private static NotificationTemplate existing() {
        return NotificationTemplate.create("tpl-1", EVENT, NotificationChannel.EMAIL,
                NotificationCategory.SECURITY, "vi-VN", "Subject", "Hello {{name}}", CLOCK);
    }

    private static TemplateCommands.CreateTemplate createCommand(String locale) {
        return new TemplateCommands.CreateTemplate(EVENT, NotificationChannel.EMAIL,
                NotificationCategory.SECURITY, locale, "Subject", "Hello {{name}}");
    }

    @Test
    void createExtractsPlaceholdersAndPublishesEvent() {
        when(repository.findByEventChannelLocale(EVENT, NotificationChannel.EMAIL, "vi-VN"))
                .thenReturn(Optional.empty());
        when(repository.save(any(NotificationTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationTemplate created = service().create(createCommand("vi-VN"));

        assertThat(created.getPlaceholders()).containsExactly("name");
        assertThat(created.getVersion()).isEqualTo(1);
        assertThat(created.getCreatedAt()).isEqualTo(NOW);
        verify(eventPublisher).publish(any(java.util.Collection.class));
    }

    @Test
    void createDefaultsLocaleToVietnamese() {
        when(repository.findByEventChannelLocale(EVENT, NotificationChannel.EMAIL, "vi-VN"))
                .thenReturn(Optional.empty());
        when(repository.save(any(NotificationTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service().create(createCommand(null)).getLocale()).isEqualTo("vi-VN");
    }

    @Test
    void createRejectsDuplicateEventChannelLocale() {
        when(repository.findByEventChannelLocale(EVENT, NotificationChannel.EMAIL, "vi-VN"))
                .thenReturn(Optional.of(existing()));

        NotificationTemplateService service = service();
        TemplateCommands.CreateTemplate command = createCommand("vi-VN");

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(NotificationException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        NotificationErrorCode.TEMPLATE_DUPLICATE.getCode());
        verify(repository, never()).save(any());
    }

    @Test
    void updateBumpsVersionAndReExtractsPlaceholders() {
        when(repository.findById("tpl-1")).thenReturn(Optional.of(existing()));
        when(repository.save(any(NotificationTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationTemplate updated = service().update(
                new TemplateCommands.UpdateTemplate("tpl-1", "New subject", "Hi {{firstName}}"));

        assertThat(updated.getVersion()).isEqualTo(2);
        assertThat(updated.getPlaceholders()).containsExactly("firstName");
        assertThat(updated.getSubject()).isEqualTo("New subject");
    }

    @Test
    void updateThrowsWhenTemplateMissing() {
        when(repository.findById("nope")).thenReturn(Optional.empty());

        NotificationTemplateService service = service();
        TemplateCommands.UpdateTemplate command = new TemplateCommands.UpdateTemplate("nope", null, "content");

        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(NotificationException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        NotificationErrorCode.TEMPLATE_NOT_FOUND.getCode());
    }

    @Test
    void getReturnsTemplate() {
        when(repository.findById("tpl-1")).thenReturn(Optional.of(existing()));

        assertThat(service().get("tpl-1").getTemplateId()).isEqualTo("tpl-1");
    }

    @Test
    void getThrowsWhenMissing() {
        when(repository.findById("nope")).thenReturn(Optional.empty());

        NotificationTemplateService service = service();

        assertThatThrownBy(() -> service.get("nope")).isInstanceOf(NotificationException.class);
    }

    @Test
    void listDelegatesToRepository() {
        when(repository.findAll(10)).thenReturn(List.of(existing()));

        assertThat(service().list(10)).hasSize(1);
    }
}
