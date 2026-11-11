package com.aionn.payment.application.policy;

import java.time.ZoneId;

public interface PaymentAnalyticsPolicy {

    ZoneId zone();

    int defaultRangeDays();

    long maxRangeDays();
}
