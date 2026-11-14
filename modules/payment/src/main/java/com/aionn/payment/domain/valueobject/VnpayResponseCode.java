package com.aionn.payment.domain.valueobject;

public enum VnpayResponseCode {
    INVALID_SIGNATURE("97", "Invalid Signature"),
    CONFIRM_SUCCESS("00", "Confirm Success"),
    ORDER_NOT_FOUND("01", "Order not Found"),
    UNKNOWN_ERROR("99", "Unknown error");

    private final String code;
    private final String message;

    VnpayResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
