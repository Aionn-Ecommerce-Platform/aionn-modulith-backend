package com.aionn.ucp.adapter.rest.dto.order;

import com.aionn.ucp.adapter.rest.dto.UcpMessage;
import com.aionn.ucp.adapter.rest.dto.cart.UcpItemResponse;
import com.aionn.ucp.adapter.rest.dto.cart.UcpResponseMetadata;
import com.aionn.ucp.adapter.rest.dto.cart.UcpTotalResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * UCP Order capability DTO models conforming to release 2026-08-25
 * schemas/shopping/order.json.
 */
public final class UcpOrderModels {

        private UcpOrderModels() {
        }

        /**
         * Top-level order response object.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record UcpOrderResponse(
                        @JsonProperty("ucp") UcpResponseMetadata ucp,
                        @JsonProperty("id") String id,
                        @JsonProperty("label") String label,
                        @JsonProperty("checkout_id") String checkoutId,
                        @JsonProperty("permalink_url") String permalinkUrl,
                        @JsonProperty("line_items") List<UcpOrderLineItemDto> lineItems,
                        @JsonProperty("fulfillment") UcpOrderFulfillmentDto fulfillment,
                        @JsonProperty("currency") String currency,
                        @JsonProperty("totals") List<UcpTotalResponse> totals,
                        @JsonProperty("adjustments") List<Map<String, Object>> adjustments,
                        @JsonProperty("policies") List<Map<String, Object>> policies,
                        @JsonProperty("messages") List<UcpMessage> messages,
                        @JsonProperty("attribution") Map<String, Object> attribution) {
        }

        /**
         * Line item inside a completed or in-flight order.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record UcpOrderLineItemDto(
                        @JsonProperty("id") String id,
                        @JsonProperty("item") UcpItemResponse item,
                        @JsonProperty("quantity") UcpOrderQuantityDto quantity,
                        @JsonProperty("totals") List<UcpTotalResponse> totals,
                        @JsonProperty("status") String status) {
        }

        /**
         * Tracks original, active, and fulfilled quantities for a line item.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record UcpOrderQuantityDto(
                        @JsonProperty("original") Integer original,
                        @JsonProperty("total") Integer total,
                        @JsonProperty("fulfilled") Integer fulfilled) {
        }

        /**
         * Fulfillment state capturing expectations and shipment events.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record UcpOrderFulfillmentDto(
                        @JsonProperty("expectations") List<UcpExpectationDto> expectations,
                        @JsonProperty("events") List<Map<String, Object>> events) {
        }

        /**
         * Buyer-facing delivery expectation representing grouped line items.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record UcpExpectationDto(
                        @JsonProperty("id") String id,
                        @JsonProperty("line_items") List<UcpExpectationLineItemDto> lineItems,
                        @JsonProperty("method_type") String methodType,
                        @JsonProperty("destination") UcpPostalAddressDto destination,
                        @JsonProperty("description") String description,
                        @JsonProperty("fulfillable_on") String fulfillableOn) {
        }

        /**
         * References a line item and quantity inside a fulfillment expectation.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record UcpExpectationLineItemDto(
                        @JsonProperty("id") String id,
                        @JsonProperty("quantity") Integer quantity) {
        }

        /**
         * Destination postal address for shipping fulfillment.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record UcpPostalAddressDto(
                        @JsonProperty("street_address") String streetAddress,
                        @JsonProperty("extended_address") String extendedAddress,
                        @JsonProperty("address_locality") String addressLocality,
                        @JsonProperty("address_region") String addressRegion,
                        @JsonProperty("address_country") String addressCountry,
                        @JsonProperty("postal_code") String postalCode,
                        @JsonProperty("first_name") String firstName,
                        @JsonProperty("last_name") String lastName,
                        @JsonProperty("phone_number") String phoneNumber) {
        }
}
