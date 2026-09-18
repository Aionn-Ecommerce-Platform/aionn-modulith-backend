package com.aionn.ucp.adapter.rest.dto.cart;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UcpResponseMetadata(
        @JsonProperty("version") String version,
        @JsonProperty("status") String status,
        @JsonProperty("capabilities") Map<String, List<Map<String, Object>>> capabilities) {

    public static UcpResponseMetadata success(String version) {
        return new UcpResponseMetadata(version, "success", null);
    }
}
