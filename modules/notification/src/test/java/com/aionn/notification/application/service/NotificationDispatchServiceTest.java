package com.aionn.notification.application.service;

import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.port.out.NotificationPersistencePort;
import com.aionn.notification.application.port.out.NotificationSubscriptionPersistencePort;
import com.aionn.notification.application.port.out.NotificationTemplatePersistencePort;
import com.aionn.notification.domain.model.Notification;
import com.aionn.notification.domain.model.NotificationSubscription;
import com.aionn.notification.domain.model.NotificationTemplate;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    @Mock
    NotificationPersistencePort notificationRepository;
    @Mock
    NotificationTemplatePersistencePort templateRepository;
    @Mock
    NotificationSubscriptionPersistencePort subscriptionRepository;

    @InjectMocks
    NotificationDispatchService notificationDispatchService;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.resetLocaleContext();
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("Should resolve locale to en-US when thread context is en")
    void shouldResolveLocaleToEnUS_whenThreadContextIsEn() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("en"));

        // Given
        NotificationSubscription subscription = NotificationSubscription.createDefault("user-1");
        when(subscriptionRepository.findByUserId("user-1")).thenReturn(Optional.of(subscription));

        NotificationTemplate template = NotificationTemplate.create(
                "temp-1", "event-1", NotificationChannel.EMAIL, NotificationCategory.SECURITY,
                "en-US", "Subject", "Content"
        );
        when(templateRepository.findByEventChannelLocale("event-1", NotificationChannel.EMAIL, "en-US"))
                .thenReturn(Optional.of(template));

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationCommands.SendByEvent command = new NotificationCommands.SendByEvent(
                "user-1", "event-1", NotificationCategory.SECURITY,
                List.of(NotificationChannel.EMAIL), null, null, Map.of()
        );

        // When
        try {
            notificationDispatchService.sendByEvent(command);
        } catch (Exception ignored) {
            // RecipientResolver might fail, but we only want to verify template lookup locale
        }

        // Then
        verify(templateRepository).findByEventChannelLocale("event-1", NotificationChannel.EMAIL, "en-US");
    }

    @Test
    @DisplayName("Should resolve locale to vi-VN when thread context is vi")
    void shouldResolveLocaleToViVN_whenThreadContextIsVi() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));

        // Given
        NotificationSubscription subscription = NotificationSubscription.createDefault("user-1");
        when(subscriptionRepository.findByUserId("user-1")).thenReturn(Optional.of(subscription));

        NotificationTemplate template = NotificationTemplate.create(
                "temp-1", "event-1", NotificationChannel.EMAIL, NotificationCategory.SECURITY,
                "vi-VN", "Subject", "Content"
        );
        when(templateRepository.findByEventChannelLocale("event-1", NotificationChannel.EMAIL, "vi-VN"))
                .thenReturn(Optional.of(template));

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationCommands.SendByEvent command = new NotificationCommands.SendByEvent(
                "user-1", "event-1", NotificationCategory.SECURITY,
                List.of(NotificationChannel.EMAIL), null, null, Map.of()
        );

        // When
        try {
            notificationDispatchService.sendByEvent(command);
        } catch (Exception ignored) {
        }

        // Then
        verify(templateRepository).findByEventChannelLocale("event-1", NotificationChannel.EMAIL, "vi-VN");
    }
}
