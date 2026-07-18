package com.aionn.ordering.application.usecase.order;

import com.aionn.ordering.application.dto.order.command.ConfirmPreparationCommand;
import com.aionn.ordering.application.dto.order.result.OrderResult;

import com.aionn.ordering.application.port.in.order.ConfirmPreparationInputPort;
import com.aionn.ordering.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfirmPreparationUseCase implements ConfirmPreparationInputPort {

    private final OrderService orderService;

    @Override
    public OrderResult execute(ConfirmPreparationCommand command) {
        return orderService.confirmPreparation(command);
    }
}