package com.aionn.ucp.adapter.rest.dto.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UcpCatalogSearchRequest(
        @JsonProperty("query") String query,
        @JsonProperty("filters") UcpCatalogModels.UcpSearchFiltersDto filters,
        @JsonProperty("pagination") UcpCatalogModels.UcpPaginationRequestDto pagination,
        @JsonProperty("context") Map<String, Object> context,
        @JsonProperty("signals") Map<String, Object> signals,
        @JsonProperty("attribution") Map<String, Object> attribution) {
}
