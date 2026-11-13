package com.aionn.notification.adapter.rest.dto.template.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTemplateRequest(
        @Size(max = 255) String subject,
        @NotBlank String content) {
}

