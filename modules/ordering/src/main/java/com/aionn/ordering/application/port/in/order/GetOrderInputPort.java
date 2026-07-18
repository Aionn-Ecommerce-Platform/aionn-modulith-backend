package com.aionn.ordering.application.port.in.order;

import com.aionn.ordering.application.dto.order.result.OrderResult;

public interface GetOrderInputPort {
    OrderResult execute(String orderId, String userId);
}