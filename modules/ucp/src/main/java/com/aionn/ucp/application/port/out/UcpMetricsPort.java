package com.aionn.ucp.application.port.out;

import java.time.Duration;

/** Outbound port for UCP protocol observability metrics. */
public interface UcpMetricsPort {

    /** @param status {@code success}, {@code client_error} or {@code server_error} */
    void recordRequest(String capability, String operation, String status);

    void recordLatency(String capability, String operation, Duration duration);

    void recordWebhookDispatch(String eventType, boolean success);
}
