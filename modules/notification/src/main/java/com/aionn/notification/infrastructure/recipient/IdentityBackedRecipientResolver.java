package com.aionn.notification.infrastructure.recipient;

import com.aionn.notification.application.port.out.DeviceTokenPersistencePort;
import com.aionn.notification.application.port.out.RecipientResolver;
import com.aionn.notification.domain.exception.NotificationErrorCode;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.model.DeviceToken;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.contracts.port.identity.RecipientResolverPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class IdentityBackedRecipientResolver implements RecipientResolver {

    private final RecipientResolverPort identityResolver;
    private final DeviceTokenPersistencePort deviceTokenPersistencePort;

    @Override
    public String resolve(String userId, NotificationChannel channel) {
        return switch (channel) {
            case EMAIL, SMS, IN_APP -> identityResolver.resolve(userId, channel.name());
            case PUSH -> firstActiveDeviceToken(userId);
        };
    }

    @Override
    public List<String> resolveAll(String userId, NotificationChannel channel) {
        if (channel != NotificationChannel.PUSH) {
            return List.of(resolve(userId, channel));
        }
        List<DeviceToken> tokens = deviceTokenPersistencePort.findActiveByUserId(userId);
        if (tokens.isEmpty()) {
            throw new NotificationException(NotificationErrorCode.DEVICE_TOKEN_NOT_FOUND,
                    "No active device token registered for user " + userId);
        }
        return tokens.stream().map(DeviceToken::getDeviceToken).distinct().toList();
    }

    private String firstActiveDeviceToken(String userId) {
        List<DeviceToken> tokens = deviceTokenPersistencePort.findActiveByUserId(userId);
        if (tokens.isEmpty()) {
            throw new NotificationException(NotificationErrorCode.DEVICE_TOKEN_NOT_FOUND,
                    "No active device token registered for user " + userId);
        }
        return tokens.get(0).getDeviceToken();
    }
}
