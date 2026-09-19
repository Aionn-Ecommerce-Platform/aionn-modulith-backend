package com.aionn.ucp.application.port.out;

/**
 * Port for dispatching outbound UCP webhook notifications to external
 * platforms.
 */
public interface UcpWebhookDispatcherPort {

    /**
     * Dispatches a webhook event to the destination URL.
     *
     * @param webhookUrl destination endpoint
     * @param event      payload to deliver
     * @return true if successfully delivered (HTTP 2xx), false otherwise
     */
    boolean dispatch(String webhookUrl, UcpWebhookEvent event);
}
