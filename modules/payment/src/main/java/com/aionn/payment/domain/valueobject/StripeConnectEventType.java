package com.aionn.payment.domain.valueobject;

public enum StripeConnectEventType {
    ACCOUNT_UPDATED("account.updated");

    private final String eventType;

    StripeConnectEventType(String eventType) {
        this.eventType = eventType;
    }

    public String eventType() {
        return eventType;
    }
}
