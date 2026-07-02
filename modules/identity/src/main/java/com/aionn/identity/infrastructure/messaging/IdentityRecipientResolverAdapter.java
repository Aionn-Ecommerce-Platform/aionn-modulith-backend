package com.aionn.identity.infrastructure.messaging;

import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.sharedkernel.common.exception.NotFoundException;
import com.aionn.sharedkernel.common.exception.ValidationException;
import com.aionn.sharedkernel.integration.port.identity.RecipientResolverPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "notification.recipient", name = "provider", havingValue = "identity", matchIfMissing = true)
public class IdentityRecipientResolverAdapter implements RecipientResolverPort {

    private final UserPersistencePort userPersistencePort;

    @Override
    public String resolve(String userId, String channel) {
        if ("IN_APP".equals(channel)) {
            return "in-app:" + userId;
        }

        var user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "Identity",
                        userId,
                        "Identity user not found for notification userId=" + userId));

        return switch (channel) {
            case "EMAIL" -> requireValue(user.getEmail(), "email");
            case "SMS" -> requireValue(user.getPhone(), "phone");
            case "PUSH" -> throw new ValidationException(
                    "Identity",
                    "RECIPIENT_PUSH_NOT_SUPPORTED",
                    "PUSH channel is resolved by the notification-side device-token resolver, not by identity");
            default -> throw new IllegalArgumentException("Unknown channel: " + channel);
        };
    }

    private static String requireValue(String value, String channelName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(
                    "Identity",
                    "RECIPIENT_CHANNEL_UNAVAILABLE",
                    "User has no " + channelName + " recipient configured");
        }
        return value;
    }
}
