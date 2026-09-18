package com.aionn.ucp.adapter.rest.dto.cart;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UcpItemResponse(
        @JsonProperty("id") String id,
        @JsonProperty("title") String title,
        @JsonProperty("price") long price,
        @JsonProperty("image_url") String imageUrl) {
}
