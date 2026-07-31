package com.aionn.notification.application.service;

import com.aionn.notification.application.dto.subscription.command.SubscriptionCommands;
import com.aionn.notification.application.port.out.DeviceTokenPersistencePort;
import com.aionn.notification.application.port.out.NotificationSubscriptionPersistencePort;
import com.aionn.notification.domain.exception.NotificationErrorCode;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.model.DeviceToken;
import com.aionn.notification.domain.model.NotificationSubscription;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSubscriptionServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private NotificationSubscriptionPersistencePort subscriptionRepository;
    @Mock
    private DeviceTokenPersistencePort deviceTokenRepository;
    @Mock
    private EventPublisher eventPublisher;

    private NotificationSubscriptionService service() {
        return new NotificationSubscriptionService(subscriptionRepository, deviceTokenRepository,
                eventPublisher, CLOCK);
    }

    private static DeviceToken token(String tokenId) {
        return DeviceToken.register(tokenId, "user-1", "fcm-token", "android", CLOCK);
    }

    @Test
    void updateChannelPersistsOptOut() {
        when(subscriptionRepository.findByUserId("user-1"))
                .thenReturn(Optional.of(NotificationSubscription.createDefault("user-1", CLOCK)));
        when(subscriptionRepository.save(any(NotificationSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationSubscription saved = service().updateChannel(new SubscriptionCommands.UpdateChannel(
                "user-1", NotificationCategory.PROMOTION, NotificationChannel.EMAIL, false));

        assertThat(saved.isEnabled(NotificationCategory.PROMOTION, NotificationChannel.EMAIL)).isFalse();
        assertThat(saved.getUpdatedAt()).isEqualTo(NOW);
        verify(eventPublisher).publish(anyCollection());
    }

    @Test
    void updateChannelCreatesDefaultSubscriptionWhenMissing() {
        when(subscriptionRepository.findByUserId("user-1")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(NotificationSubscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationSubscription saved = service().updateChannel(new SubscriptionCommands.UpdateChannel(
                "user-1", NotificationCategory.PROMOTION, NotificationChannel.PUSH, false));

        assertThat(saved.getUserId()).isEqualTo("user-1");
    }

    @Test
    void updateChannelRejectsDisablingMandatoryCategory() {
        when(subscriptionRepository.findByUserId("user-1"))
                .thenReturn(Optional.of(NotificationSubscription.createDefault("user-1", CLOCK)));

        NotificationSubscriptionService service = service();
        SubscriptionCommands.UpdateChannel command = new SubscriptionCommands.UpdateChannel(
                "user-1", NotificationCategory.SECURITY, NotificationChannel.EMAIL, false);

        assertThatThrownBy(() -> service.updateChannel(command))
                .isInstanceOf(NotificationException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        NotificationErrorCode.SUBSCRIPTION_REQUIRED_CHANNEL.getCode());
    }

    @Test
    void getReturnsDefaultsWithoutPersistingWhenMissing() {
        when(subscriptionRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        assertThat(service().get("user-1").getUserId()).isEqualTo("user-1");
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void getReturnsStoredSubscription() {
        when(subscriptionRepository.findByUserId("user-1"))
                .thenReturn(Optional.of(NotificationSubscription.createDefault("user-1", CLOCK)));

        assertThat(service().get("user-1").getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void registerDeviceTokenIsIdempotent() {
        when(deviceTokenRepository.findByUserAndToken("user-1", "fcm-token"))
                .thenReturn(Optional.of(token("tok-existing")));

        DeviceToken result = service().registerDeviceToken(
                new SubscriptionCommands.RegisterDeviceToken("user-1", "fcm-token", "android"));

        assertThat(result.getTokenId()).isEqualTo("tok-existing");
        verify(deviceTokenRepository, never()).save(any());
        verify(eventPublisher, never()).publish(anyCollection());
    }

    @Test
    void registerDeviceTokenStoresNewToken() {
        when(deviceTokenRepository.findByUserAndToken("user-1", "fcm-token"))
                .thenReturn(Optional.empty());
        when(deviceTokenRepository.save(any(DeviceToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeviceToken result = service().registerDeviceToken(
                new SubscriptionCommands.RegisterDeviceToken("user-1", "fcm-token", "android"));

        assertThat(result.isActive()).isTrue();
        assertThat(result.getRegisteredAt()).isEqualTo(NOW);
        verify(eventPublisher).publish(anyCollection());
    }

    @Test
    void removeDeviceTokenDeactivatesOwnedToken() {
        when(deviceTokenRepository.findById("tok-1")).thenReturn(Optional.of(token("tok-1")));

        service().removeDeviceToken(new SubscriptionCommands.RemoveDeviceToken("user-1", "tok-1"));

        verify(deviceTokenRepository).save(any(DeviceToken.class));
    }

    @Test
    void removeDeviceTokenRejectsForeignToken() {
        when(deviceTokenRepository.findById("tok-1")).thenReturn(Optional.of(token("tok-1")));

        NotificationSubscriptionService service = service();
        SubscriptionCommands.RemoveDeviceToken command = new SubscriptionCommands.RemoveDeviceToken("intruder",
                "tok-1");

        assertThatThrownBy(() -> service.removeDeviceToken(command))
                .isInstanceOf(NotificationException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        NotificationErrorCode.NOTIFICATION_FORBIDDEN.getCode());
        verify(deviceTokenRepository, never()).save(any());
    }

    @Test
    void removeDeviceTokenThrowsWhenMissing() {
        when(deviceTokenRepository.findById("nope")).thenReturn(Optional.empty());

        NotificationSubscriptionService service = service();
        SubscriptionCommands.RemoveDeviceToken command = new SubscriptionCommands.RemoveDeviceToken("user-1", "nope");

        assertThatThrownBy(() -> service.removeDeviceToken(command))
                .isInstanceOf(NotificationException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        NotificationErrorCode.DEVICE_TOKEN_NOT_FOUND.getCode());
    }

    @Test
    void listDeviceTokensDelegatesToRepository() {
        when(deviceTokenRepository.findActiveByUserId("user-1"))
                .thenReturn(List.of(token("tok-1"), token("tok-2")));

        assertThat(service().listDeviceTokens("user-1")).hasSize(2);
    }
}
