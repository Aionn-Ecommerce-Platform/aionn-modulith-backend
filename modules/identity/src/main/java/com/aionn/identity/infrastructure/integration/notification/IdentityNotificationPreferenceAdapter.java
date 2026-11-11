package com.aionn.identity.infrastructure.integration.notification;

import com.aionn.contracts.port.notification.NotificationPreferenceQueryPort;
import com.aionn.identity.application.port.out.preference.UserPreferencePersistencePort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Exposes the legacy account-settings notification toggles to the notification module.
 * Preferences are stored as a JSON blob keyed by order/promotion/chat/system; malformed or
 * missing values fall back to enabled so a storage problem never silently drops notifications.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityNotificationPreferenceAdapter implements NotificationPreferenceQueryPort {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final UserPreferencePersistencePort preferencePersistencePort;

    @Override
    public NotificationPreferenceSnapshot find(String userId) {
        if (userId == null || userId.isBlank()) {
            return NotificationPreferenceSnapshot.defaults();
        }
        String json = preferencePersistencePort.findById(userId)
                .map(preference -> preference.notificationSettings())
                .orElse(null);
        if (json == null || json.isBlank()) {
            return NotificationPreferenceSnapshot.defaults();
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            return new NotificationPreferenceSnapshot(
                    node.path("order").asBoolean(true),
                    node.path("promotion").asBoolean(true),
                    node.path("chat").asBoolean(true),
                    node.path("system").asBoolean(true));
        } catch (Exception ex) {
            log.warn("Unreadable notification settings for user {}: {}", userId, ex.getMessage());
            return NotificationPreferenceSnapshot.defaults();
        }
    }
}
