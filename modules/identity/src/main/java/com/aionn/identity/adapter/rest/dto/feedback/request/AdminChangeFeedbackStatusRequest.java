package com.aionn.identity.adapter.rest.dto.feedback.request;

import com.aionn.identity.domain.valueobject.FeedbackStatus;
import jakarta.validation.constraints.NotNull;

public record AdminChangeFeedbackStatusRequest(
        @NotNull FeedbackStatus status) {
}
