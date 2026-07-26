package com.aionn.promotion.infrastructure.observability;

import com.aionn.promotion.application.port.out.observability.PromotionMetricsPort;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerPromotionMetricsAdapter implements PromotionMetricsPort {

    private final MeterRegistry registry;

    public MicrometerPromotionMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void campaignLifecycle(String transition) {
        registry.counter("promotion.campaign.lifecycle", "transition", transition).increment();
    }

    @Override
    public void voucherLifecycle(String transition) {
        registry.counter("promotion.voucher.lifecycle", "transition", transition).increment();
    }

    @Override
    public void userVoucherLifecycle(String transition) {
        registry.counter("promotion.user_voucher.lifecycle", "transition", transition).increment();
    }

    @Override
    public void scheduledTransition(String type, int count) {
        if (count > 0) {
            registry.counter("promotion.scheduled_transition", "type", type).increment(count);
        }
    }

    @Override
    public void autoReleased(int count) {
        if (count > 0) {
            registry.counter("promotion.voucher.auto_released").increment(count);
        }
    }
}
