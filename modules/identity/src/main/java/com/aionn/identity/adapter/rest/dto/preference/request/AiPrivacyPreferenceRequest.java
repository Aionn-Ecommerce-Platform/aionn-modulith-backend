package com.aionn.identity.adapter.rest.dto.preference.request;

import com.aionn.sharedkernel.adapter.web.validation.ValidJson;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiPrivacyPreferenceRequest(
        @NotBlank(message = "AI privacy settings JSON is required")
        @Size(max = 4096, message = "AI privacy settings JSON must be at most 4096 characters")
        @ValidJson(message = "AI privacy settings must be well-formed JSON")
        String aiPrivacySettingsJson) {
}
