package com.aionn.ordering.application.port.out.observability;

public interface OrderingMetricsPort {

    void orderLifecycle(String transition);

    void cartLifecycle(String transition);

    void returnLifecycle(String transition);

    void placeOrderOutcome(String outcome);

    void autoCancelled(int count);

    void gatewayOutcome(String gateway, String outcome);
}
