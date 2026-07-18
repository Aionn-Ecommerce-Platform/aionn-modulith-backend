package com.aionn.ordering.application.port.in.order;

import com.aionn.ordering.application.dto.order.result.OrderResult;
import java.util.List;

public interface ListOrdersInputPort {
    List<OrderResult> execute(String requesterId, String type, String status, int limit);
}