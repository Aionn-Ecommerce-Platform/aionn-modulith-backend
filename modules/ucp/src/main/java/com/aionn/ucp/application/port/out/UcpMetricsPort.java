package com.aionn.ucp.application.port.out;

import java.time.Duration;

/**
 * Outbound port for recording UCP protocol observability metrics.
 */
public interface UcpMetricsPort {

    /**
     * Records an incoming UCP protocol request.
     *
     * @param capability the capability name (e.g. "order", "cart", "checkout",
     *                   "catalog", "identity_linking")
     * @param operation  the operation name (e.g. "lookup", "create", "search",
     *                   "link")
     * @param status     outcome status (e.g. "success", "client_error",
     *                   "server_error")
     */
    void recordRequest(String capability, String operation, String status);

    /**
     * Records execution latency for a UCP operation.
     *
     * @param capability the capability name
     * @param operation  the operation name
     * @param duration   the execution duration
     */
    void recordLatency(String capability, String operation, Duration duration);

    /**
     * Records an outbound webhook dispatch attempt.
     *
     * @param eventType the event type (e.g. "order.placed", "order.shipped")
     * @param success   true if delivered successfully, false otherwise
     */
    void recordWebhookDispatch(String eventType, boolean success);
}
