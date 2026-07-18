package com.aionn.ordering.application.usecase.order;

import com.aionn.ordering.application.dto.order.result.OrderResult;
import com.aionn.ordering.application.port.in.order.ListOrdersInputPort;
import com.aionn.ordering.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListOrdersUseCase implements ListOrdersInputPort {

    private final OrderService orderService;

    @Override
    public List<OrderResult> execute(String requesterId, String type, String status, int limit) {
        if ("MERCHANT".equalsIgnoreCase(type)) {
            return orderService.listByMerchantOwner(requesterId, status, limit);
        } else {
            return orderService.listByUser(requesterId, status, limit);
        }
    }
}