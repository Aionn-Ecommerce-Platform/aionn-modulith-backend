package com.aionn.catalog.adapter.rest.dto.question.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnswerQuestionRequest(
        @NotBlank @Size(min = 1, max = 2000) String answer) {
}
