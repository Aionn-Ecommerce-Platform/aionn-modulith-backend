package com.aionn.shipping.infrastructure.persistence.adapter.webhook;

import com.aionn.shipping.application.port.out.CarrierWebhookIdempotencyPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcCarrierWebhookIdempotencyAdapter implements CarrierWebhookIdempotencyPort {

    private final JdbcTemplate jdbc;

    @Override
    public boolean claim(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return true;
        }
        return jdbc.update("""
                INSERT INTO processed_carrier_webhooks (event_id)
                VALUES (?)
                ON CONFLICT (event_id) DO NOTHING
                """, eventId) == 1;
    }
}
