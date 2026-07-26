package com.aionn.notification.infrastructure.observability;

import com.aionn.notification.application.port.out.observability.NotificationMetricsPort;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerNotificationMetricsAdapter implements NotificationMetricsPort {

    private final MeterRegistry registry;

    public MicrometerNotificationMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void notificationLifecycle(String transition) {
        registry.counter("notification.lifecycle", "transition", transition).increment();
    }

    @Override
    public void deliveryOutcome(String channel, String outcome) {
        registry.counter("notification.delivery.outcome",
                "channel", channel, "outcome", outcome).increment();
    }

    @Override
    public void retryAttempt(int succeeded) {
        if (succeeded > 0) {
            registry.counter("notification.retry.succeeded").increment(succeeded);
        }
    }

    @Override
    public void templateLifecycle(String transition) {
        registry.counter("notification.template.lifecycle", "transition", transition).increment();
    }

    @Override
    public void subscriptionLifecycle(String transition) {
        registry.counter("notification.subscription.lifecycle", "transition", transition).increment();
    }

    @Override
    public void providerLifecycle(String transition) {
        registry.counter("notification.provider.lifecycle", "transition", transition).increment();
    }
}
