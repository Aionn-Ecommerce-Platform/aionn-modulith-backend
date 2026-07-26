package com.aionn.ordering.application.usecase.order;

import com.aionn.ordering.application.dto.order.result.OrderResult;
import com.aionn.ordering.application.mapper.OrderResultMapper;
import com.aionn.ordering.application.port.in.order.ListOrdersInputPort;
import com.aionn.ordering.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListOrdersUseCase implements ListOrdersInputPort {

    private final OrderService orderService;
    private final OrderResultMapper orderResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<OrderResult> execute(String requesterId, String type, String status, int limit) {
        if ("MERCHANT".equalsIgnoreCase(type)) {
            return orderResultMapper.toResults(orderService.listByMerchantOwner(requesterId, status, limit));
        } else {
            return orderResultMapper.toResults(orderService.listByUser(requesterId, status, limit));
        }
    }
}
