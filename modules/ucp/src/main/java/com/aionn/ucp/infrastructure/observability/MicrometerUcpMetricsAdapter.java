package com.aionn.ucp.infrastructure.observability;

import com.aionn.ucp.application.port.out.UcpMetricsPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Micrometer-backed implementation of UcpMetricsPort for UCP observability.
 */
@Component
public class MicrometerUcpMetricsAdapter implements UcpMetricsPort {

    private static final String DEFAULT_UNKNOWN = "unknown";
    private final MeterRegistry registry;

    public MicrometerUcpMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void recordRequest(String capability, String operation, String status) {
        registry.counter("ucp.requests.total",
                "capability", capability != null ? capability : DEFAULT_UNKNOWN,
                "operation", operation != null ? operation : DEFAULT_UNKNOWN,
                "status", status != null ? status : DEFAULT_UNKNOWN).increment();
    }

    @Override
    public void recordLatency(String capability, String operation, Duration duration) {
        if (duration != null) {
            Timer.builder("ucp.requests.duration")
                    .tag("capability", capability != null ? capability : DEFAULT_UNKNOWN)
                    .tag("operation", operation != null ? operation : DEFAULT_UNKNOWN)
                    .register(registry)
                    .record(duration);
        }
    }

    @Override
    public void recordWebhookDispatch(String eventType, boolean success) {
        registry.counter("ucp.webhooks.total",
                "event_type", eventType != null ? eventType : DEFAULT_UNKNOWN,
                "success", Boolean.toString(success)).increment();
    }
}
