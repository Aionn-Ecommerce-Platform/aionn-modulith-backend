package com.aionn.identity.adapter.rest.dto.feedback.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitFeedbackRequest(
        @Size(max = 30) String category,
        @Size(max = 200) String subject,
        @NotBlank @Size(max = 5000) String content,
        @Min(1) @Max(5) Integer rating,
        @Email @Size(max = 150) String contactEmail,
        @Size(max = 30) String contactPhone) {
}
