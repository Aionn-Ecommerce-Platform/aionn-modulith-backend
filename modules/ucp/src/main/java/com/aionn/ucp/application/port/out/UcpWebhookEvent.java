package com.aionn.ucp.application.port.out;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

/**
 * Outbound webhook event model dispatched to external platforms for order
 * lifecycle updates.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UcpWebhookEvent(
                @JsonProperty("event_id") String eventId,
                @JsonProperty("event_type") String eventType,
                @JsonProperty("order_id") String orderId,
                @JsonProperty("occurred_at") Instant occurredAt,
                @JsonProperty("data") Map<String, Object> data) {
}
