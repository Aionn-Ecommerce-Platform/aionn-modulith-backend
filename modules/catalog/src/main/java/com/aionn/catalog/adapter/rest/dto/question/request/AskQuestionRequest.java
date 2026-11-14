package com.aionn.catalog.adapter.rest.dto.question.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskQuestionRequest(
        @NotBlank @Size(min = 5, max = 2000) String question) {
}
