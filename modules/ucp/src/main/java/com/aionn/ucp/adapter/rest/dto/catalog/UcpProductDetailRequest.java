package com.aionn.ucp.adapter.rest.dto.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UcpProductDetailRequest(
                @JsonProperty("id") String id,
                @JsonProperty("selected") List<UcpCatalogModels.UcpSelectedOptionDto> selected,
                @JsonProperty("preferences") List<String> preferences,
                @JsonProperty("filters") UcpCatalogModels.UcpSearchFiltersDto filters,
                @JsonProperty("context") Map<String, Object> context,
                @JsonProperty("signals") Map<String, Object> signals,
                @JsonProperty("attribution") Map<String, Object> attribution) {
}
