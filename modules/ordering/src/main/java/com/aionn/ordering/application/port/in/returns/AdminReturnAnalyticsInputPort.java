package com.aionn.ordering.application.port.in.returns;

import com.aionn.ordering.application.dto.returns.result.ReturnAnalyticsResult;

import java.time.LocalDate;

public interface AdminReturnAnalyticsInputPort {
    ReturnAnalyticsResult execute(LocalDate from, LocalDate to);
}
