package com.aionn.ucp.adapter.rest.dto.cart.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UcpItemRequest(
        @NotBlank(message = "Item ID is required") @JsonProperty("id") String id) {
}
