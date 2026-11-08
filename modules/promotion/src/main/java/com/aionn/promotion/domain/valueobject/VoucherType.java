package com.aionn.promotion.domain.valueobject;

/** What part of the checkout a voucher reduces. */
public enum VoucherType {
    /** Reduces goods subtotal (existing behaviour). */
    ORDER_DISCOUNT,
    /** Reduces shipping fee up to the voucher's discountAmount cap. */
    FREE_SHIPPING
}
