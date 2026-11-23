package com.aionn.ordering.application.port.in.order;

import com.aionn.ordering.application.dto.order.result.OrderPageResult;
import com.aionn.ordering.application.dto.order.result.OrderResult;

import java.time.Instant;
import java.util.List;

public interface ListOrdersInputPort {
    List<OrderResult> execute(String requesterId, String type, String status, int limit);

    OrderPageResult executePaged(
            String requesterId,
            String type,
            String status,
            String search,
            Instant from,
            Instant to,
            int page,
            int limit);
}
