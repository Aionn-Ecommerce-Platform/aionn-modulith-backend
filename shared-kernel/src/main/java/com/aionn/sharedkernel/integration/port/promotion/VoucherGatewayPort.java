package com.aionn.sharedkernel.integration.port.promotion;

import java.math.BigDecimal;

/**
 * Outbound port for applying a voucher/discount during checkout.
 *
 * <p>
 * Used synchronously by the Ordering module so the order total can reflect any
 * applicable discount before placement.
 * </p>
 */
public interface VoucherGatewayPort {

    Discount apply(String userId, String voucherCode, BigDecimal lineSubtotal, String currency);

    record Discount(BigDecimal amount, String currency, boolean valid, String reason) {
    }
}
