package com.aionn.ordering.application.port.in.order;

import com.aionn.ordering.application.dto.order.result.PlatformOrderAnalyticsResult;
import java.time.LocalDate;

public interface GetPlatformOrderAnalyticsInputPort {
    PlatformOrderAnalyticsResult execute(LocalDate from, LocalDate to);
}