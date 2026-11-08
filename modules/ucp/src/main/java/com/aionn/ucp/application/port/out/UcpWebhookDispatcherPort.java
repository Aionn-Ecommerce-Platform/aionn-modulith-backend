package com.aionn.ucp.application.port.out;

/** Dispatches outbound UCP webhook notifications. */
public interface UcpWebhookDispatcherPort {

    /** @return true when the destination answered 2xx */
    boolean dispatch(String webhookUrl, UcpWebhookEvent event);
}
