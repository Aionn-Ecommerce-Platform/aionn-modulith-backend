package com.aionn.ordering.application.usecase.order;

import com.aionn.ordering.application.dto.order.command.PlaceOrderCommand;
import com.aionn.ordering.application.dto.order.result.OrderResult;

import com.aionn.ordering.application.port.in.order.PlaceOrderInputPort;
import com.aionn.ordering.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceOrderUseCase implements PlaceOrderInputPort {

    private final OrderService orderService;

    @Override
    public OrderResult execute(PlaceOrderCommand command) {
        return orderService.placeOrder(command);
    }
}