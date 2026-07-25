package com.aionn.shipping.application.port.out.observability;

public interface ShippingMetricsPort {

    void shipmentLifecycle(String transition);

    void rateLifecycle(String transition);

    void carrierOutcome(String operation, String outcome);

    void quoteOutcome(String source);
}
