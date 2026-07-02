package com.aionn.identity.application.dto.preference.result;

import java.time.LocalDateTime;

public record UserPreferenceResult(
                String userId,
                String language,
                String currency,
                String timezone,
                String theme,
                String notificationSettings,
                String aiPrivacySettings,
                LocalDateTime updatedAt) {
}

