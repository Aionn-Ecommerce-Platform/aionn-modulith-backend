package com.aionn.identity.adapter.rest.dto.consent.request;

import jakarta.validation.constraints.NotBlank;

public record TermsConsentRequest(
        @NotBlank(message = "Version is required")
        String version) {
}


