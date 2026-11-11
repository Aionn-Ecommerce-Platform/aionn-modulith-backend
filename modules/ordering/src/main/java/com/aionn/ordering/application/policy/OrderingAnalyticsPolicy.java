package com.aionn.ordering.application.policy;

import java.time.ZoneId;

public interface OrderingAnalyticsPolicy {

    ZoneId zone();

    int merchantDefaultRangeDays();

    int platformDefaultRangeDays();

    int topMerchantsLimit();

    int maxTopProductsLimit();
}
