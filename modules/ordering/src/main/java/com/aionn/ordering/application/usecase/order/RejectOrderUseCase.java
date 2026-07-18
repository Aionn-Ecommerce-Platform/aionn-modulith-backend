package com.aionn.ordering.application.usecase.order;

import com.aionn.ordering.application.dto.order.command.RejectOrderCommand;
import com.aionn.ordering.application.dto.order.result.OrderResult;
import com.aionn.ordering.application.port.in.order.RejectOrderInputPort;
import com.aionn.ordering.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RejectOrderUseCase implements RejectOrderInputPort {

    private final OrderService orderService;

    @Override
    public OrderResult execute(RejectOrderCommand command) {
        return orderService.rejectByMerchant(command);
    }
}