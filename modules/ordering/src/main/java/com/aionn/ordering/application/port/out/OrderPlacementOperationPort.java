package com.aionn.ordering.application.port.out;

public interface OrderPlacementOperationPort {

    record Operation(String orderId, String requestHash, boolean completed) {}

    Operation start(String userId, String idempotencyKey, String requestHash, String proposedOrderId);

    void complete(String userId, String idempotencyKey, String orderId);
}
