package com.aionn.ucp.adapter.rest.dto.catalog;

import com.aionn.ucp.adapter.rest.dto.UcpMessage;
import com.aionn.ucp.adapter.rest.dto.cart.UcpResponseMetadata;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UcpCatalogLookupResponse(
        @JsonProperty("ucp") UcpResponseMetadata ucp,
        @JsonProperty("products") List<UcpCatalogModels.UcpProductDto> products,
        @JsonProperty("actions") Map<String, Object> actions,
        @JsonProperty("messages") List<UcpMessage> messages,
        @JsonProperty("policies") List<Map<String, Object>> policies) {
}
