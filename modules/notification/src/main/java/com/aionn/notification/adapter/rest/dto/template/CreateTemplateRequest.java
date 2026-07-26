package com.aionn.notification.adapter.rest.dto.template;

import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTemplateRequest(
        @NotBlank @Size(max = 100) String eventType,
        @NotNull NotificationChannel channel,
        @NotNull NotificationCategory category,
        @Size(max = 20) String locale,
        @Size(max = 255) String subject,
        @NotBlank String content) {
}

