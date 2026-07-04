package com.aionn.identity.adapter.rest.dto.feedback.request;

import com.aionn.identity.domain.valueobject.FeedbackStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminReplyFeedbackRequest(
        @NotBlank @Size(max = 4000) String reply,
        FeedbackStatus newStatus) {
}
