package com.aionn.identity.adapter.rest.dto.preference.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GeneralPreferenceRequest(
                @NotBlank(message = "Language is required") @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "Language must be a BCP 47 tag, e.g. 'en' or 'vi-VN'") String language,

                @NotBlank(message = "Currency is required") @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be an ISO-4217 code, e.g. 'VND'") String currency,

                @NotBlank(message = "Timezone is required") @Size(max = 50, message = "Timezone must be at most 50 characters") String timezone,

                @NotBlank(message = "Theme is required") @Pattern(regexp = "^(light|dark|system)$", message = "Theme must be light, dark or system") String theme) {
}
