package com.aionn.identity.adapter.rest.dto.preference.request;

import com.aionn.identity.adapter.rest.validation.ValidJson;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotificationPreferenceRequest(
        @NotBlank(message = "Notification settings JSON is required")
        @Size(max = 4096, message = "Notification settings JSON must be at most 4096 characters")
        @ValidJson(message = "Notification settings must be well-formed JSON")
        String notificationSettingsJson) {
}
