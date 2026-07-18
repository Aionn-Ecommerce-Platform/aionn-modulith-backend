package com.aionn.ordering.application.usecase.order;

import com.aionn.ordering.application.dto.order.command.ConfirmShippedCommand;
import com.aionn.ordering.application.dto.order.result.OrderResult;
import com.aionn.ordering.application.port.in.order.ConfirmShippedInputPort;
import com.aionn.ordering.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfirmShippedUseCase implements ConfirmShippedInputPort {

    private final OrderService orderService;

    @Override
    public OrderResult execute(ConfirmShippedCommand command) {
        return orderService.markShipped(command);
    }
}