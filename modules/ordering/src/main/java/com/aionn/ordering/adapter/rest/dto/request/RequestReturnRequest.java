package com.aionn.ordering.adapter.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RequestReturnRequest(
        @NotBlank @Size(max = 1000) String reason,
        @Size(max = 2048) String evidenceUrl,
        @NotEmpty @Valid List<ItemRequest> items) {

    public RequestReturnRequest(String reason, String evidenceUrl) {
        this(reason, evidenceUrl, List.of());
    }

    public record ItemRequest(@NotBlank String skuId, @Positive int qty) {
    }
}
