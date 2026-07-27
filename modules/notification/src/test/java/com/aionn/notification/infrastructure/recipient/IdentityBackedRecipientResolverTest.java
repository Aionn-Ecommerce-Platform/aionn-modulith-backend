package com.aionn.notification.infrastructure.recipient;

import com.aionn.notification.application.port.out.DeviceTokenPersistencePort;
import com.aionn.notification.domain.exception.NotificationErrorCode;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.model.DeviceToken;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.sharedkernel.integration.port.identity.RecipientResolverPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityBackedRecipientResolverTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-01T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private RecipientResolverPort identityResolver;
    @Mock
    private DeviceTokenPersistencePort deviceTokenRepository;

    @InjectMocks
    private IdentityBackedRecipientResolver resolver;

    private static DeviceToken token(String tokenId, String value) {
        return DeviceToken.register(tokenId, "user-1", value, "android", CLOCK);
    }

    @Test
    void emailDelegatesToIdentity() {
        when(identityResolver.resolve("user-1", "EMAIL")).thenReturn("a@b.c");

        assertThat(resolver.resolve("user-1", NotificationChannel.EMAIL)).isEqualTo("a@b.c");
    }

    @Test
    void smsDelegatesToIdentity() {
        when(identityResolver.resolve("user-1", "SMS")).thenReturn("0912345678");

        assertThat(resolver.resolve("user-1", NotificationChannel.SMS)).isEqualTo("0912345678");
    }

    @Test
    void inAppDelegatesToIdentity() {
        when(identityResolver.resolve("user-1", "IN_APP")).thenReturn("user-1");

        assertThat(resolver.resolve("user-1", NotificationChannel.IN_APP)).isEqualTo("user-1");
    }

    @Test
    void pushUsesFirstActiveDeviceToken() {
        when(deviceTokenRepository.findActiveByUserId("user-1"))
                .thenReturn(List.of(token("tok-1", "fcm-1"), token("tok-2", "fcm-2")));

        assertThat(resolver.resolve("user-1", NotificationChannel.PUSH)).isEqualTo("fcm-1");
    }

    @Test
    void pushThrowsWhenNoActiveDeviceToken() {
        when(deviceTokenRepository.findActiveByUserId("user-1")).thenReturn(List.of());

        assertThatThrownBy(() -> resolver.resolve("user-1", NotificationChannel.PUSH))
                .isInstanceOf(NotificationException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        NotificationErrorCode.DEVICE_TOKEN_NOT_FOUND.getCode());
    }
}
