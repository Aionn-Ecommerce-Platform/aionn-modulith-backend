package com.aionn.payment.domain.valueobject;

public enum PaymentGatewayKind {
    STRIPE("GATEWAY_ERROR"),
    VNPAY("VNPAY_ERROR");

    private final String defaultFailureCode;

    PaymentGatewayKind(String defaultFailureCode) {
        this.defaultFailureCode = defaultFailureCode;
    }

    public String defaultFailureCode() {
        return defaultFailureCode;
    }
}
