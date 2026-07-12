package com.aionn.identity.application.dto.preference.result;

import java.time.Instant;

public record UserPreferenceResult(
                String userId,
                String language,
                String currency,
                String timezone,
                String theme,
                String notificationSettings,
                String aiPrivacySettings,
                Instant updatedAt) {
}

