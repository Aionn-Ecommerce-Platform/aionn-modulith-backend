package com.aionn.ordering.application.usecase.order;

import com.aionn.ordering.application.dto.order.result.OrderPageResult;
import com.aionn.ordering.application.dto.order.result.OrderResult;
import com.aionn.ordering.application.mapper.OrderResultMapper;
import com.aionn.ordering.application.port.in.order.ListOrdersInputPort;
import com.aionn.ordering.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
        }
        return orderResultMapper.toResults(orderService.listByUser(requesterId, status, limit));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderPageResult executePaged(
            String requesterId,
            String type,
            String status,
            String search,
            Instant from,
            Instant to,
            int page,
            int limit) {
        if ("MERCHANT".equalsIgnoreCase(type)) {
            OrderService.OrderPage result = orderService.searchMerchantPage(
                    requesterId, status, search, from, to, page, limit);
            return new OrderPageResult(orderResultMapper.toResults(result.content()), result.totalElements());
        }
        OrderService.OrderPage userPage = orderService.pageByUser(requesterId, status, page, limit);
        return new OrderPageResult(
                orderResultMapper.toResults(userPage.content()), userPage.totalElements());
    }
}
