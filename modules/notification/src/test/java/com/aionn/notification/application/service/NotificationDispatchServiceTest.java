package com.aionn.notification.application.service;

import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.port.out.NotificationPersistencePort;
import com.aionn.notification.application.port.out.NotificationSubscriptionPersistencePort;
import com.aionn.notification.application.port.out.NotificationTemplatePersistencePort;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.model.Notification;
import com.aionn.notification.domain.model.NotificationSubscription;
import com.aionn.notification.domain.model.NotificationTemplate;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.notification.domain.valueobject.NotificationStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationDispatchServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String EVENT = "identity.password-changed";

    @Mock
    private NotificationPersistencePort notificationRepository;
    @Mock
    private NotificationTemplatePersistencePort templateRepository;
    @Mock
    private NotificationSubscriptionPersistencePort subscriptionRepository;
    @Mock
    private EventPublisher eventPublisher;

    private NotificationDispatchService service() {
        return new NotificationDispatchService(notificationRepository, templateRepository,
                subscriptionRepository, eventPublisher, CLOCK);
    }

    private static NotificationTemplate template(NotificationChannel channel) {
        return NotificationTemplate.create("tpl-" + channel, EVENT, channel,
                NotificationCategory.SECURITY, "vi-VN", "Subject", "Hello {{name}}", CLOCK);
    }

    private static Notification pending(String notiId) {
        return Notification.create(notiId, "user-1", "tpl-1", NotificationChannel.EMAIL,
                NotificationCategory.SECURITY, "Subject", "Hello", null, CLOCK);
    }

    private void echoSave() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private NotificationCommands.SendByEvent byEvent(List<NotificationChannel> channels) {
        return new NotificationCommands.SendByEvent("user-1", EVENT,
                NotificationCategory.SECURITY, channels, "vi-VN", "camp-1",
                Map.of("name", "Tran"));
    }

    @Test
    void prepareByEventCreatesOnePendingNotificationPerEnabledChannel() {
        when(subscriptionRepository.findByUserId("user-1"))
                .thenReturn(Optional.of(NotificationSubscription.createDefault("user-1", CLOCK)));
        when(templateRepository.findByEventChannelLocale(eq(EVENT), any(), anyString()))
                .thenAnswer(invocation -> Optional.of(template(invocation.getArgument(1))));
        echoSave();

        List<Notification> prepared = service()
                .prepareByEvent(byEvent(List.of(NotificationChannel.EMAIL, NotificationChannel.SMS)));

        assertThat(prepared).hasSize(2);
        assertThat(prepared).allMatch(n -> n.getStatus() == NotificationStatus.PENDING);
        assertThat(prepared.get(0).getContent()).isEqualTo("Hello Tran");
        assertThat(prepared.get(0).getCampaignId()).isEqualTo("camp-1");
    }

    @Test
    void prepareByEventCreatesDefaultSubscriptionWhenMissing() {
        when(subscriptionRepository.findByUserId("user-1")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(NotificationSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(templateRepository.findByEventChannelLocale(eq(EVENT), any(), anyString()))
                .thenAnswer(invocation -> Optional.of(template(invocation.getArgument(1))));
        echoSave();

        assertThat(service().prepareByEvent(byEvent(List.of(NotificationChannel.EMAIL)))).hasSize(1);
        verify(subscriptionRepository).save(any(NotificationSubscription.class));
    }

    @Test
    void prepareByEventFansOutToEveryChannelWhenNoneRequested() {
        when(subscriptionRepository.findByUserId("user-1"))
                .thenReturn(Optional.of(NotificationSubscription.createDefault("user-1", CLOCK)));
        when(templateRepository.findByEventChannelLocale(eq(EVENT), any(), anyString()))
                .thenAnswer(invocation -> Optional.of(template(invocation.getArgument(1))));
        echoSave();

        assertThat(service().prepareByEvent(byEvent(null)))
                .hasSize(NotificationChannel.values().length);
    }

    @Test
    void prepareByEventSkipsChannelsWithoutTemplate() {
        when(subscriptionRepository.findByUserId("user-1"))
                .thenReturn(Optional.of(NotificationSubscription.createDefault("user-1", CLOCK)));
        when(templateRepository.findByEventChannelLocale(eq(EVENT), any(), anyString()))
                .thenReturn(Optional.empty());

        assertThat(service().prepareByEvent(byEvent(List.of(NotificationChannel.EMAIL)))).isEmpty();
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void prepareByEventSkipsChannelsDisabledBySubscription() {
        NotificationSubscription subscription = NotificationSubscription.createDefault("user-1", CLOCK);
        subscription.update(NotificationCategory.PROMOTION, NotificationChannel.EMAIL, false, CLOCK);
        when(subscriptionRepository.findByUserId("user-1")).thenReturn(Optional.of(subscription));

        List<Notification> prepared = service().prepareByEvent(new NotificationCommands.SendByEvent(
                "user-1", EVENT, NotificationCategory.PROMOTION,
                List.of(NotificationChannel.EMAIL), "vi-VN", null, Map.of("name", "Tran")));

        assertThat(prepared).isEmpty();
    }

    @Test
    void prepareByEventFallsBackToDefaultLocale() {
        when(subscriptionRepository.findByUserId("user-1"))
                .thenReturn(Optional.of(NotificationSubscription.createDefault("user-1", CLOCK)));
        when(templateRepository.findByEventChannelLocale(EVENT, NotificationChannel.EMAIL, "fr-FR"))
                .thenReturn(Optional.empty());
        when(templateRepository.findByEventChannelLocale(EVENT, NotificationChannel.EMAIL, "vi-VN"))
                .thenReturn(Optional.of(template(NotificationChannel.EMAIL)));
        echoSave();

        List<Notification> prepared = service().prepareByEvent(new NotificationCommands.SendByEvent(
                "user-1", EVENT, NotificationCategory.SECURITY,
                List.of(NotificationChannel.EMAIL), "fr-FR", null, Map.of("name", "Tran")));

        assertThat(prepared).hasSize(1);
    }

    @Test
    void resolvesTheEnglishLocaleFromTheRequestContextWhenNoneIsRequested() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        try {
            when(subscriptionRepository.findByUserId("user-1"))
                    .thenReturn(Optional.of(NotificationSubscription.createDefault("user-1", CLOCK)));
            when(templateRepository.findByEventChannelLocale(EVENT, NotificationChannel.EMAIL, "en-US"))
                    .thenReturn(Optional.of(template(NotificationChannel.EMAIL)));
            echoSave();

            List<Notification> prepared = service().prepareByEvent(new NotificationCommands.SendByEvent(
                    "user-1", EVENT, NotificationCategory.SECURITY,
                    List.of(NotificationChannel.EMAIL), null, null, Map.of("name", "Tran")));

            assertThat(prepared).hasSize(1);
            verify(templateRepository).findByEventChannelLocale(EVENT, NotificationChannel.EMAIL, "en-US");
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    @Test
    void fallsBackToTheDefaultLocaleForANonEnglishRequestContext() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));
        try {
            when(subscriptionRepository.findByUserId("user-1"))
                    .thenReturn(Optional.of(NotificationSubscription.createDefault("user-1", CLOCK)));
            when(templateRepository.findByEventChannelLocale(EVENT, NotificationChannel.EMAIL, "vi-VN"))
                    .thenReturn(Optional.of(template(NotificationChannel.EMAIL)));
            echoSave();

            List<Notification> prepared = service().prepareByEvent(new NotificationCommands.SendByEvent(
                    "user-1", EVENT, NotificationCategory.SECURITY,
                    List.of(NotificationChannel.EMAIL), null, null, Map.of("name", "Tran")));

            assertThat(prepared).hasSize(1);
            verify(templateRepository).findByEventChannelLocale(EVENT, NotificationChannel.EMAIL, "vi-VN");
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    @Test
    void prepareDirectUsesExplicitChannel() {
        when(templateRepository.findByEventChannelLocale(EVENT, NotificationChannel.SMS, "vi-VN"))
                .thenReturn(Optional.of(template(NotificationChannel.SMS)));
        echoSave();

        Notification prepared = service().prepareDirect(new NotificationCommands.SendDirectByEvent(
                "user-1", EVENT, NotificationCategory.SECURITY, NotificationChannel.SMS,
                "0912345678", "vi-VN", null, Map.of("name", "Tran")));

        assertThat(prepared.getChannel()).isEqualTo(NotificationChannel.SMS);
        assertThat(prepared.getStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    void prepareDirectThrowsWhenTemplateMissing() {
        when(templateRepository.findByEventChannelLocale(anyString(), any(), anyString()))
                .thenReturn(Optional.empty());

        NotificationDispatchService service = service();
        NotificationCommands.SendDirectByEvent command = new NotificationCommands.SendDirectByEvent(
                "user-1", EVENT, NotificationCategory.SECURITY, NotificationChannel.SMS,
                "0912345678", "vi-VN", null, Map.of());

        assertThatThrownBy(() -> service.prepareDirect(command))
                .isInstanceOf(NotificationException.class);
    }

    @Test
    void recordSentMovesNotificationToSent() {
        when(notificationRepository.findById("noti-1")).thenReturn(Optional.of(pending("noti-1")));
        echoSave();

        Notification saved = service().recordSent(new NotificationCommands.RecordSent("noti-1"));

        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(saved.getSentAt()).isEqualTo(NOW);
        verify(eventPublisher).publish(anyCollection());
    }

    @Test
    void recordFailedIncrementsRetryCount() {
        when(notificationRepository.findById("noti-1")).thenReturn(Optional.of(pending("noti-1")));
        echoSave();

        Notification saved = service().recordFailed(
                new NotificationCommands.RecordFailed("noti-1", "SMTP_DOWN:timeout"));

        assertThat(saved.getRetryCount()).isEqualTo(1);
        assertThat(saved.getLastFailureReason()).isEqualTo("SMTP_DOWN:timeout");
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    void recordFailedMarksFailedAfterMaxRetries() {
        Notification notification = pending("noti-1");
        when(notificationRepository.findById("noti-1")).thenReturn(Optional.of(notification));
        echoSave();

        NotificationDispatchService service = service();
        for (int attempt = 0; attempt < 3; attempt++) {
            service.recordFailed(new NotificationCommands.RecordFailed("noti-1", "boom"));
        }

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.canRetry()).isFalse();
    }

    @Test
    void recordDeliveryUnknownFailsTheNotificationWithoutRepublishing() {
        when(notificationRepository.findById("noti-1")).thenReturn(Optional.of(pending("noti-1")));
        echoSave();

        Notification saved = service().recordDeliveryUnknown("noti-1", "gateway timeout");

        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(saved.getRetryCount()).isEqualTo(1);
        assertThat(saved.getLastFailureReason()).isEqualTo("DELIVERY_OUTCOME_UNKNOWN:gateway timeout");
        // An ambiguous provider outcome must not fan out a failure event that could
        // trigger another delivery attempt for a message that may already have been sent.
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void recordDeliveryUnknownThrowsWhenNotificationMissing() {
        when(notificationRepository.findById("nope")).thenReturn(Optional.empty());

        NotificationDispatchService service = service();

        assertThatThrownBy(() -> service.recordDeliveryUnknown("nope", "gateway timeout"))
                .isInstanceOf(NotificationException.class);
    }

    @Test
    void recordSentThrowsWhenNotificationMissing() {
        when(notificationRepository.findById("nope")).thenReturn(Optional.empty());

        NotificationDispatchService service = service();
        NotificationCommands.RecordSent command = new NotificationCommands.RecordSent("nope");

        assertThatThrownBy(() -> service.recordSent(command))
                .isInstanceOf(NotificationException.class);
    }

    @Test
    void markReadRequiresOwnership() {
        when(notificationRepository.findById("noti-1")).thenReturn(Optional.of(pending("noti-1")));

        NotificationDispatchService service = service();
        NotificationCommands.MarkRead command = new NotificationCommands.MarkRead("intruder", "noti-1");

        assertThatThrownBy(() -> service.markRead(command))
                .isInstanceOf(NotificationException.class);
    }

    @Test
    void markReadMovesNotificationToRead() {
        Notification notification = pending("noti-1");
        notification.markSent(CLOCK);
        when(notificationRepository.findById("noti-1")).thenReturn(Optional.of(notification));
        echoSave();

        Notification saved = service().markRead(new NotificationCommands.MarkRead("user-1", "noti-1"));

        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.READ);
        assertThat(saved.getReadAt()).isEqualTo(NOW);
    }

    @Test
    void deleteSoftDeletesNotification() {
        Notification notification = pending("noti-1");
        notification.markSent(CLOCK);
        when(notificationRepository.findById("noti-1")).thenReturn(Optional.of(notification));
        echoSave();

        Notification saved = service().delete(new NotificationCommands.MarkDeleted("user-1", "noti-1"));

        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.DELETED);
        assertThat(saved.getDeletedAt()).isEqualTo(NOW);
    }

    @Test
    void getReturnsOwnedNotification() {
        when(notificationRepository.findById("noti-1")).thenReturn(Optional.of(pending("noti-1")));

        assertThat(service().get("user-1", "noti-1").getNotiId()).isEqualTo("noti-1");
    }

    @Test
    void listMineDelegatesToRepository() {
        when(notificationRepository.findByUser("user-1", 20))
                .thenReturn(List.of(pending("noti-1"), pending("noti-2")));

        assertThat(service().listMine("user-1", 20)).hasSize(2);
    }

    @Test
    void findRetryableDelegatesToRepository() {
        when(notificationRepository.findRetryable(50)).thenReturn(List.of(pending("noti-1")));

        assertThat(service().findRetryable(50)).hasSize(1);
    }
}
