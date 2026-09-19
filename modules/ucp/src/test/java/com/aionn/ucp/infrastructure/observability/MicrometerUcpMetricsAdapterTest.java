package com.aionn.ucp.infrastructure.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerUcpMetricsAdapterTest {

    private MeterRegistry registry;
    private MicrometerUcpMetricsAdapter adapter;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        adapter = new MicrometerUcpMetricsAdapter(registry);
    }

    @Test
    void recordRequestIncrementsCounter() {
        adapter.recordRequest("order", "lookup", "success");

        double count = registry.get("ucp.requests.total")
                .tag("capability", "order")
                .tag("operation", "lookup")
                .tag("status", "success")
                .counter()
                .count();

        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void recordLatencyRecordsDuration() {
        adapter.recordLatency("cart", "create", Duration.ofMillis(120));

        double totalTime = registry.get("ucp.requests.duration")
                .tag("capability", "cart")
                .tag("operation", "create")
                .timer()
                .totalTime(java.util.concurrent.TimeUnit.MILLISECONDS);

        assertThat(totalTime).isGreaterThanOrEqualTo(120.0);
    }

    @Test
    void recordWebhookDispatchIncrementsCounter() {
        adapter.recordWebhookDispatch("order.shipped", true);

        double count = registry.get("ucp.webhooks.total")
                .tag("event_type", "order.shipped")
                .tag("success", "true")
                .counter()
                .count();

        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void recordWithNullParametersUsesDefaults() {
        adapter.recordRequest(null, null, null);

        double count = registry.get("ucp.requests.total")
                .tag("capability", "unknown")
                .tag("operation", "unknown")
                .tag("status", "unknown")
                .counter()
                .count();

        assertThat(count).isEqualTo(1.0);

        adapter.recordLatency(null, null, Duration.ofMillis(50));
        double latencyCount = registry.get("ucp.requests.duration")
                .tag("capability", "unknown")
                .tag("operation", "unknown")
                .timer()
                .count();
        assertThat(latencyCount).isEqualTo(1L);

        // Null duration should do nothing
        adapter.recordLatency("cart", "create", null);

        adapter.recordWebhookDispatch(null, false);
        double webhookCount = registry.get("ucp.webhooks.total")
                .tag("event_type", "unknown")
                .tag("success", "false")
                .counter()
                .count();
        assertThat(webhookCount).isEqualTo(1.0);
    }
}
