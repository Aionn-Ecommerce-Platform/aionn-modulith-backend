package com.aionn.ucp.adapter.rest.dto.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public final class UcpCatalogModels {

        private UcpCatalogModels() {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record UcpProductDto(
                        @JsonProperty("id") String id,
                        @JsonProperty("title") String title,
                        @JsonProperty("description") UcpDescriptionDto description,
                        @JsonProperty("price_range") UcpPriceRangeDto priceRange,
                        @JsonProperty("variants") List<UcpVariantDto> variants,
                        @JsonProperty("media") List<UcpMediaDto> media,
                        @JsonProperty("options") List<UcpProductOptionDto> options,
                        @JsonProperty("categories") List<Map<String, Object>> categories,
                        @JsonProperty("rating") Map<String, Object> rating,
                        @JsonProperty("tags") List<String> tags,
                        @JsonProperty("handle") String handle,
                        @JsonProperty("url") String url,
                        @JsonProperty("selected") List<UcpSelectedOptionDto> selected) {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record UcpVariantDto(
                        @JsonProperty("id") String id,
                        @JsonProperty("title") String title,
                        @JsonProperty("description") UcpDescriptionDto description,
                        @JsonProperty("price") UcpPriceDto price,
                        @JsonProperty("sku") String sku,
                        @JsonProperty("availability") UcpAvailabilityDto availability,
                        @JsonProperty("options") List<UcpSelectedOptionDto> options,
                        @JsonProperty("media") List<UcpMediaDto> media,
                        @JsonProperty("inputs") List<UcpInputCorrelationDto> inputs) {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record UcpDescriptionDto(
                        @JsonProperty("plain") String plain) {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record UcpPriceRangeDto(
                        @JsonProperty("min") UcpPriceDto min,
                        @JsonProperty("max") UcpPriceDto max) {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record UcpPriceDto(
                        @JsonProperty("amount") Long amount,
                        @JsonProperty("currency") String currency) {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record UcpAvailabilityDto(
                        @JsonProperty("available") Boolean available,
                        @JsonProperty("status") String status) {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record UcpMediaDto(
                        @JsonProperty("type") String type,
                        @JsonProperty("url") String url,
                        @JsonProperty("alt_text") String altText) {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record UcpSelectedOptionDto(
                        @JsonProperty("name") String name,
                        @JsonProperty("label") String label,
                        @JsonProperty("id") String id) {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record UcpProductOptionDto(
                        @JsonProperty("name") String name,
                        @JsonProperty("values") List<UcpOptionValueDto> values) {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record UcpOptionValueDto(
                        @JsonProperty("label") String label,
                        @JsonProperty("id") String id,
                        @JsonProperty("available") Boolean available,
                        @JsonProperty("exists") Boolean exists) {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record UcpInputCorrelationDto(
                        @JsonProperty("id") String id,
                        @JsonProperty("match") String match) {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record UcpSearchFiltersDto(
                        @JsonProperty("categories") List<String> categories,
                        @JsonProperty("price") UcpPriceFilterDto price) {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record UcpPriceFilterDto(
                        @JsonProperty("min") Long min,
                        @JsonProperty("max") Long max) {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record UcpPaginationRequestDto(
                        @JsonProperty("limit") Integer limit,
                        @JsonProperty("cursor") String cursor) {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record UcpPaginationResponseDto(
                        @JsonProperty("has_next_page") Boolean hasNextPage,
                        @JsonProperty("cursor") String cursor,
                        @JsonProperty("total_count") Integer totalCount) {
        }
}
