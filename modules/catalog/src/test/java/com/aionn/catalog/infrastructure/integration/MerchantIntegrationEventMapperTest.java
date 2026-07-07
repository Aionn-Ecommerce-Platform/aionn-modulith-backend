package com.aionn.catalog.infrastructure.integration;

import com.aionn.catalog.domain.event.MerchantEvents;
import com.aionn.sharedkernel.integration.event.catalog.MerchantActivatedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.catalog.MerchantClosedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.catalog.MerchantSuspendedIntegrationEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantIntegrationEventMapperTest {

    private final MerchantIntegrationEventMapper mapper = new MerchantIntegrationEventMapper();

    @Test
    void mapsMerchantSuspendedEvent() {
        Instant now = Instant.now();
        MerchantEvents.MerchantSuspended domain = new MerchantEvents.MerchantSuspended(
                "m-1", "policy violation", "admin-1", now, now);

        MerchantSuspendedIntegrationEvent result = mapper.toIntegrationEvent(domain);

        assertThat(result.eventId()).isNotBlank();
        assertThat(result.merchantId()).isEqualTo("m-1");
        assertThat(result.reason()).isEqualTo("policy violation");
        assertThat(result.occurredAt()).isEqualTo(now);
    }

    @Test
    void mapsMerchantClosedEvent() {
        Instant now = Instant.now();
        MerchantEvents.MerchantClosed domain = new MerchantEvents.MerchantClosed(
                "m-1", "voluntary shutdown", now, now);

        MerchantClosedIntegrationEvent result = mapper.toIntegrationEvent(domain);

        assertThat(result.merchantId()).isEqualTo("m-1");
        assertThat(result.reason()).isEqualTo("voluntary shutdown");
        assertThat(result.occurredAt()).isEqualTo(now);
    }

    @Test
    void mapsMerchantActivatedEvent() {
        Instant now = Instant.now();
        MerchantEvents.MerchantActivated domain = new MerchantEvents.MerchantActivated(
                "m-1", "admin-1", "reinstate", now, now);

        MerchantActivatedIntegrationEvent result = mapper.toIntegrationEvent(domain);

        assertThat(result.merchantId()).isEqualTo("m-1");
        assertThat(result.adminId()).isEqualTo("admin-1");
        assertThat(result.occurredAt()).isEqualTo(now);
    }
}
