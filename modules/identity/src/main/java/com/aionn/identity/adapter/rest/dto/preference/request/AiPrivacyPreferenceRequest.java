package com.aionn.identity.adapter.rest.dto.preference.request;

import jakarta.validation.constraints.NotBlank;

public record AiPrivacyPreferenceRequest(
        @NotBlank(message = "AI privacy settings JSON is required")
        String aiPrivacySettingsJson) {
}


