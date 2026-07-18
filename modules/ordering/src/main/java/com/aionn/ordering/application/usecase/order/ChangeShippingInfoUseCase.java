package com.aionn.ordering.application.usecase.order;

import com.aionn.ordering.application.dto.order.command.ChangeShippingInfoCommand;
import com.aionn.ordering.application.dto.order.result.OrderResult;

import com.aionn.ordering.application.port.in.order.ChangeShippingInfoInputPort;
import com.aionn.ordering.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChangeShippingInfoUseCase implements ChangeShippingInfoInputPort {

    private final OrderService orderService;

    @Override
    public OrderResult execute(ChangeShippingInfoCommand command) {
        return orderService.changeShippingInfo(command);
    }
}