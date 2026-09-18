package com.aionn.ucp.adapter.rest.dto.cart;

import com.aionn.ucp.adapter.rest.dto.UcpMessage;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UcpCartResponse(
                @JsonProperty("ucp") UcpResponseMetadata ucp,
                @JsonProperty("id") String id,
                @JsonProperty("currency") String currency,
                @JsonProperty("line_items") List<UcpLineItemResponse> lineItems,
                @JsonProperty("totals") List<UcpTotalResponse> totals,
                @JsonProperty("expires_at") String expiresAt,
                @JsonProperty("messages") List<UcpMessage> messages) {
}
