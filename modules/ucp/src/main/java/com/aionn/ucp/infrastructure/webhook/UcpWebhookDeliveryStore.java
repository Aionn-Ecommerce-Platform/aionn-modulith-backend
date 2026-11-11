package com.aionn.ucp.infrastructure.webhook;

import com.aionn.ucp.application.port.out.UcpWebhookEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UcpWebhookDeliveryStore {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public record PendingDelivery(String deliveryId, String webhookUrl, String payload, int attempts) {
    }

    public void enqueue(String webhookUrl, UcpWebhookEvent event) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO ucp_webhook_deliveries
                        (delivery_id, event_type, order_id, webhook_url, payload)
                    VALUES (?, ?, ?, ?, ?::jsonb)
                    ON CONFLICT (delivery_id) DO NOTHING
                    """,
                    event.eventId(),
                    event.eventType(),
                    event.orderId(),
                    webhookUrl,
                    objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize UCP webhook " + event.eventId(), exception);
        }
    }

    public List<PendingDelivery> findPending(int limit) {
        return jdbcTemplate.query("""
                SELECT delivery_id, webhook_url, payload, attempts
                  FROM ucp_webhook_deliveries
                 WHERE status = 'PENDING' AND next_attempt_at <= NOW()
                 ORDER BY created_at
                 LIMIT ?
                """,
                (rs, rowNum) -> toPending(rs),
                limit);
    }

    public void markDelivered(String deliveryId) {
        jdbcTemplate.update("""
                UPDATE ucp_webhook_deliveries
                   SET status = 'DELIVERED', updated_at = NOW(), last_error = NULL
                 WHERE delivery_id = ?
                """, deliveryId);
    }

    public void markFailed(String deliveryId, int attempts, Instant nextAttemptAt, String error, boolean dead) {
        jdbcTemplate.update("""
                UPDATE ucp_webhook_deliveries
                   SET status = ?, attempts = ?, next_attempt_at = ?, last_error = ?, updated_at = NOW()
                 WHERE delivery_id = ?
                """,
                dead ? "DEAD" : "PENDING",
                attempts,
                java.sql.Timestamp.from(nextAttemptAt),
                truncate(error),
                deliveryId);
    }

    private static PendingDelivery toPending(ResultSet rs) throws SQLException {
        return new PendingDelivery(
                rs.getString("delivery_id"),
                rs.getString("webhook_url"),
                rs.getString("payload"),
                rs.getInt("attempts"));
    }

    private static String truncate(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= 2000 ? error : error.substring(0, 2000);
    }
}
