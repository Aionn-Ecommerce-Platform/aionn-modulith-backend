package com.aionn.notification.application.service;

import com.aionn.notification.application.dto.provider.command.ProviderCommands;
import com.aionn.notification.application.port.out.NotificationProviderPersistencePort;
import com.aionn.notification.domain.exception.NotificationErrorCode;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.model.NotificationProvider;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationProviderServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private NotificationProviderPersistencePort repository;
    @Mock
    private EventPublisher eventPublisher;

    private NotificationProviderService service() {
        return new NotificationProviderService(repository, eventPublisher, CLOCK);
    }

    private static NotificationProvider existing() {
        return NotificationProvider.configure("prov-1", NotificationChannel.EMAIL, "smtp",
                Map.of("host", "localhost"), 60, "admin-1", CLOCK);
    }

    @Test
    void configureStoresActiveProviderAndPublishesEvent() {
        when(repository.save(any(NotificationProvider.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationProvider saved = service().configure(new ProviderCommands.ConfigureProvider(
                NotificationChannel.SMS, "twilio", Map.of("sid", "abc"), 30, "admin-1"));

        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getProviderType()).isEqualTo("twilio");
        assertThat(saved.getRateLimitPerMinute()).isEqualTo(30);
        assertThat(saved.getCreatedAt()).isEqualTo(NOW);
        verify(eventPublisher).publish(any(java.util.Collection.class));
    }

    @Test
    void configureRejectsBlankProviderType() {
        NotificationProviderService service = service();
        ProviderCommands.ConfigureProvider command = new ProviderCommands.ConfigureProvider(
                NotificationChannel.SMS, "  ", Map.of(), 30, "admin-1");

        assertThatThrownBy(() -> service.configure(command))
                .isInstanceOf(NotificationException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void updateAppliesOnlyProvidedFields() {
        when(repository.findById("prov-1")).thenReturn(Optional.of(existing()));
        when(repository.save(any(NotificationProvider.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationProvider saved = service().update(new ProviderCommands.UpdateProvider(
                "prov-1", null, 120, false, "admin-2"));

        assertThat(saved.getRateLimitPerMinute()).isEqualTo(120);
        assertThat(saved.isActive()).isFalse();
        assertThat(saved.getConfig()).containsEntry("host", "localhost");
    }

    @Test
    void updateReplacesConfigWhenProvided() {
        when(repository.findById("prov-1")).thenReturn(Optional.of(existing()));
        when(repository.save(any(NotificationProvider.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationProvider saved = service().update(new ProviderCommands.UpdateProvider(
                "prov-1", Map.of("host", "smtp.example.com"), null, null, "admin-2"));

        assertThat(saved.getConfig()).containsEntry("host", "smtp.example.com");
        assertThat(saved.getRateLimitPerMinute()).isEqualTo(60);
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void updateThrowsWhenProviderMissing() {
        when(repository.findById("nope")).thenReturn(Optional.empty());

        NotificationProviderService service = service();
        ProviderCommands.UpdateProvider command = new ProviderCommands.UpdateProvider(
                "nope", null, null, null, "admin-1");

        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(NotificationException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        NotificationErrorCode.PROVIDER_NOT_FOUND.getCode());
    }

    @Test
    void listAllDelegatesToRepository() {
        when(repository.findAll()).thenReturn(List.of(existing()));

        assertThat(service().listAll()).hasSize(1);
    }
}
