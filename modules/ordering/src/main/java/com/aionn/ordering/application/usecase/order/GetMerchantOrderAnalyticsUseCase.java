package com.aionn.ordering.application.usecase.order;

import com.aionn.ordering.application.dto.order.result.MerchantOrderAnalyticsResult;
import com.aionn.ordering.application.port.in.order.GetMerchantOrderAnalyticsInputPort;
import com.aionn.ordering.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GetMerchantOrderAnalyticsUseCase implements GetMerchantOrderAnalyticsInputPort {

    private final OrderService orderService;

    @Override
    public MerchantOrderAnalyticsResult execute(String ownerId, LocalDate from, LocalDate to) {
        return orderService.getMerchantAnalytics(ownerId, from, to);
    }
}