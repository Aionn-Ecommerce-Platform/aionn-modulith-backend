package com.aionn.ucp.adapter.rest.dto.cart;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UcpLineItemResponse(
                @JsonProperty("id") String id,
                @JsonProperty("item") UcpItemResponse item,
                @JsonProperty("quantity") int quantity,
                @JsonProperty("totals") List<UcpTotalResponse> totals) {
}
