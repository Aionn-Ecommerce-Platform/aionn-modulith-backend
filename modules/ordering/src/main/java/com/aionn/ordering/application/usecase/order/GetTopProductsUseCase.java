package com.aionn.ordering.application.usecase.order;

import com.aionn.ordering.application.dto.order.result.TopProductResult;
import com.aionn.ordering.application.port.in.order.GetTopProductsInputPort;
import com.aionn.ordering.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetTopProductsUseCase implements GetTopProductsInputPort {

    private final OrderService orderService;

    @Override
    public List<TopProductResult> execute(String ownerId, LocalDate from, LocalDate to, int limit) {
        return orderService.getMerchantTopProducts(ownerId, from, to, limit);
    }
}