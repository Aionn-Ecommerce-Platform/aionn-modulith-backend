package com.aionn.ordering.application.usecase.order;

import com.aionn.ordering.application.dto.order.result.OrderResult;
import com.aionn.ordering.application.port.in.order.GetOrderInputPort;
import com.aionn.ordering.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetOrderUseCase implements GetOrderInputPort {

    private final OrderService orderService;

    @Override
    public OrderResult execute(String orderId, String userId) {
        return orderService.getForRequester(orderId, userId);
    }
}