package com.aionn.ordering.application.usecase.order;

import com.aionn.ordering.application.dto.order.command.CancelOrderCommand;
import com.aionn.ordering.application.dto.order.result.OrderResult;
import com.aionn.ordering.application.port.in.order.CancelOrderInputPort;
import com.aionn.ordering.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CancelOrderUseCase implements CancelOrderInputPort {

    private final OrderService orderService;

    @Override
    public OrderResult execute(CancelOrderCommand command) {
        return orderService.cancel(command);
    }
}