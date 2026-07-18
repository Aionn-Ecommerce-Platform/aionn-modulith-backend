package com.aionn.ordering.application.port.out;

import java.math.BigDecimal;

/** Outbound port for voucher discount preview at order placement. */
public interface VoucherGateway {

    Discount apply(String userId, String merchantId, String voucherCode, String orderId,
            BigDecimal lineSubtotal, String currency);

    void release(String userId, String orderId, String reason);

    record Discount(BigDecimal amount, String currency, boolean valid, String reason) {
    }
}
