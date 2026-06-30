package com.aionn.sharedkernel.integration.port.promotion;

import java.math.BigDecimal;

public interface VoucherApplyPort {

    Discount apply(String userId, String merchantId, String voucherCode, String orderId,
            BigDecimal lineSubtotal, String currency);

    void release(String userId, String orderId, String reason);

    record Discount(BigDecimal amount, String currency, boolean applied, String reason) {
    }
}
