package com.aionn.notification.application.port.out.observability;

public interface NotificationMetricsPort {

    void notificationLifecycle(String transition);

    void deliveryOutcome(String channel, String outcome);

    void retryAttempt(int succeeded);

    void templateLifecycle(String transition);

    void subscriptionLifecycle(String transition);

    void providerLifecycle(String transition);
}
