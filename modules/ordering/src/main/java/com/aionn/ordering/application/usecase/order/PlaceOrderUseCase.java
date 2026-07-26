package com.aionn.ordering.application.usecase.order;

import com.aionn.ordering.application.dto.order.command.PlaceOrderCommand;
import com.aionn.ordering.application.dto.order.result.OrderResult;
import com.aionn.ordering.application.mapper.OrderResultMapper;
import com.aionn.ordering.application.port.in.order.PlaceOrderInputPort;
import com.aionn.ordering.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceOrderUseCase implements PlaceOrderInputPort {

    private final OrderService orderService;
    private final OrderResultMapper orderResultMapper;

    @Override
    @Transactional
    public OrderResult execute(PlaceOrderCommand command) {
        return orderResultMapper.toResult(orderService.placeOrder(command));
    }
}
