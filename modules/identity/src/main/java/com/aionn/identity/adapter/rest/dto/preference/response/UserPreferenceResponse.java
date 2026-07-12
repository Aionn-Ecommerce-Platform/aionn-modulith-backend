package com.aionn.identity.adapter.rest.dto.preference.response;

import java.time.Instant;

public record UserPreferenceResponse(
        String userId,
        String language,
        String currency,
        String timezone,
        String theme,
        String notificationSettings,
        String aiPrivacySettings,
        Instant updatedAt) {
}


