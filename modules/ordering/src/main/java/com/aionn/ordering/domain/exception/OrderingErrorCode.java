package com.aionn.ordering.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderingErrorCode {
    // Cart
    CART_NOT_FOUND("ORD_001", "Cart not found"),
    CART_FORBIDDEN("ORD_002", "Cart does not belong to this user"),
    CART_ITEM_NOT_FOUND("ORD_003", "Cart item not found"),
    CART_EMPTY("ORD_004", "Cart is empty"),

    // Order
    ORDER_NOT_FOUND("ORD_101", "Order not found"),
    ORDER_FORBIDDEN("ORD_102", "Order does not belong to this user"),
    ORDER_INVALID_STATE("ORD_103", "Order is not in a state that allows this action"),
    ORDER_ALREADY_PICKED_UP("ORD_104", "Order has already been picked up by carrier"),
    ORDER_RETURN_WINDOW_EXPIRED("ORD_105", "Return window has expired (7 days after completion)"),
    ORDER_NOT_OWNED_BY_MERCHANT("ORD_106", "Order does not belong to this merchant"),
    ORDER_RESERVATION_FAILED("ORD_107", "Stock reservation failed for one or more items"),
    ORDER_PAYMENT_FAILED("ORD_108", "Payment authorization failed"),

    // Returns
    RETURN_NOT_FOUND("ORD_201", "Order return not found"),
    RETURN_INVALID_STATE("ORD_202", "Return is not in a state that allows this action"),

    // Generic
    INVALID_ARGUMENT("ORD_900", "Invalid argument");

    private final String code;
    private final String defaultMessage;
}

