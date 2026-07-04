package com.aionn.identity.adapter.rest.dto.preference.response;

import java.time.LocalDateTime;

public record UserPreferenceResponse(
        String userId,
        String language,
        String currency,
        String timezone,
        String theme,
        String notificationSettings,
        String aiPrivacySettings,
        LocalDateTime updatedAt) {
}


