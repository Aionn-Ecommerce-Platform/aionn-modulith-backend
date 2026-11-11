package com.aionn.shipping.application.port.out;

public interface CarrierWebhookIdempotencyPort {

    /** Returns false when the event id was already processed. */
    boolean claim(String eventId);
}
