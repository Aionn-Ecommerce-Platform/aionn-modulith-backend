package com.aionn.ucp.adapter.rest.dto.cart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UcpCartRequest(
                @NotEmpty(message = "line_items cannot be empty") @Valid @JsonProperty("line_items") List<UcpLineItemRequest> lineItems,
                @JsonProperty("context") Map<String, Object> context,
                @JsonProperty("buyer") Map<String, Object> buyer) {
}
