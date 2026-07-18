package com.aionn.ordering.application.usecase.order;

import com.aionn.ordering.application.dto.order.command.ConfirmDeliveredCommand;
import com.aionn.ordering.application.dto.order.result.OrderResult;
import com.aionn.ordering.application.port.in.order.ConfirmDeliveredInputPort;
import com.aionn.ordering.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfirmDeliveredUseCase implements ConfirmDeliveredInputPort {

    private final OrderService orderService;

    @Override
    public OrderResult execute(ConfirmDeliveredCommand command) {
        return orderService.complete(command);
    }
}