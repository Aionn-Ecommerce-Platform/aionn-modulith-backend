package com.aionn.payment.application.port.out.observability;

public interface PaymentMetricsPort {

    void paymentLifecycle(String transition);

    void methodLifecycle(String transition);

    void ledgerEntry(String type);

    void providerOutcome(String gateway, String operation, String outcome);

    void reconciliation(String gateway, int matched, int mismatched);
}
