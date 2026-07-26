package com.aionn.ordering.application.usecase.returns;

import com.aionn.ordering.application.dto.returns.result.ReturnAnalyticsResult;
import com.aionn.ordering.application.port.in.returns.AdminReturnAnalyticsInputPort;
import com.aionn.ordering.application.service.OrderReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AdminReturnAnalyticsUseCase implements AdminReturnAnalyticsInputPort {

    private final OrderReturnService orderReturnService;

    @Override
    @Transactional(readOnly = true)
    public ReturnAnalyticsResult execute(LocalDate from, LocalDate to) {
        return orderReturnService.adminAnalytics(from, to);
    }
}
