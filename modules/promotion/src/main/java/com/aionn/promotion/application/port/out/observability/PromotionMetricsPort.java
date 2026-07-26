package com.aionn.promotion.application.port.out.observability;

public interface PromotionMetricsPort {

    void campaignLifecycle(String transition);

    void voucherLifecycle(String transition);

    void userVoucherLifecycle(String transition);

    void scheduledTransition(String type, int count);

    void autoReleased(int count);
}
