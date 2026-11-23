package com.aionn.ordering.application.usecase.order;

import com.aionn.ordering.application.dto.order.command.ChangeShippingAddressCommand;
import com.aionn.ordering.application.dto.order.result.OrderResult;
import com.aionn.ordering.application.mapper.OrderResultMapper;
import com.aionn.ordering.application.port.in.order.ChangeShippingAddressInputPort;
import com.aionn.ordering.application.service.OrderService;
import com.aionn.ordering.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangeShippingAddressUseCase implements ChangeShippingAddressInputPort {

    private final OrderService orderService;
    private final OrderResultMapper orderResultMapper;

    @Override
    @Transactional
    public OrderResult execute(ChangeShippingAddressCommand command) {
        Order order = orderService.changeShippingAddress(command);
        return orderResultMapper.toResult(order);
    }
}
