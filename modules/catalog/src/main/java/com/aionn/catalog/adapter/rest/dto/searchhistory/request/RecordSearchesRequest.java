package com.aionn.catalog.adapter.rest.dto.searchhistory.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RecordSearchesRequest(
        @NotEmpty @Size(max = 5) List<@NotBlank @Size(max = 200) String> queries) {
}
