package com.aionn.ordering.application.usecase.order;

import com.aionn.ordering.application.dto.order.result.OrderResult;
import com.aionn.ordering.application.mapper.OrderResultMapper;
import com.aionn.ordering.application.port.in.order.GetOrderInputPort;
import com.aionn.ordering.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetOrderUseCase implements GetOrderInputPort {

    private final OrderService orderService;
    private final OrderResultMapper orderResultMapper;

    @Override
    @Transactional(readOnly = true)
    public OrderResult execute(String orderId, String userId) {
        return orderResultMapper.toResult(orderService.getForRequester(orderId, userId));
    }
}
