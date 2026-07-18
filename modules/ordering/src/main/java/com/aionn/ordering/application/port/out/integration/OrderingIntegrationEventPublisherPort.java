package com.aionn.ordering.application.port.out.integration;

import com.aionn.ordering.domain.model.Order;

public interface OrderingIntegrationEventPublisherPort {

    void publishOrderPlaced(Order order);

    void publishOrderApproved(String orderId, String paymentId);

    void publishOrderShipped(String orderId, String shipmentId);

    void publishOrderCompleted(String orderId);

    void publishOrderCancelled(String orderId, String reasonCode, String reason, CancellationKind kind);

    enum CancellationKind {
        USER_CANCELLED,
        AUTO_CANCELLED,
        MERCHANT_REJECTED
    }
}
