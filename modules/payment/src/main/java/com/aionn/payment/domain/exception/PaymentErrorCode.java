package com.aionn.payment.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode {
    PAYMENT_NOT_FOUND("PAY_001", "Payment not found"),
    PAYMENT_INVALID_STATE("PAY_002", "Payment is not in a state that allows this action"),
    PAYMENT_DUPLICATE("PAY_003", "Duplicate payment for this idempotency key"),
    PAYMENT_GATEWAY_ERROR("PAY_004", "Payment gateway error"),
    PAYMENT_NOT_PAID("PAY_005", "Payment must be PAID before this action"),
    PAYMENT_AMOUNT_EXCEEDED("PAY_006", "Refund amount exceeds remaining payable"),

    METHOD_NOT_FOUND("PAY_101", "Payment method not found"),
    METHOD_FORBIDDEN("PAY_102", "Payment method does not belong to this user"),
    METHOD_NOT_VERIFIED("PAY_103", "Payment method must be verified before use"),

    LEDGER_NOT_FOUND("PAY_201", "Ledger entry not found"),

    INVALID_ARGUMENT("PAY_900", "Invalid argument");

    private final String code;
    private final String defaultMessage;
}

