package com.aionn.ordering.application.usecase.order;

import com.aionn.ordering.application.dto.order.result.PlatformOrderAnalyticsResult;
import com.aionn.ordering.application.port.in.order.GetPlatformOrderAnalyticsInputPort;
import com.aionn.ordering.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GetPlatformOrderAnalyticsUseCase implements GetPlatformOrderAnalyticsInputPort {

    private final OrderService orderService;

    @Override
    public PlatformOrderAnalyticsResult execute(LocalDate from, LocalDate to) {
        return orderService.getPlatformAnalytics(from, to);
    }
}