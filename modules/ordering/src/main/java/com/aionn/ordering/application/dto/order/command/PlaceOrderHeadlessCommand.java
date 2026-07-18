package com.aionn.ordering.application.dto.order.command;

import com.aionn.ordering.domain.valueobject.ShippingAddress;
import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;
import java.util.List;

/**
 * Place an order without going through the user's cart — used by headless
 * channels such as the UCP agentic checkout where the line items come
 * directly in the request payload.
 *
 * @param lines       list of {sku, qty} pairs the agent is committing to.
 * @param voucherCode optional voucher to apply (null = none).
 */
public record PlaceOrderHeadlessCommand(
        String userId,
        List<Line> lines,
        String voucherCode,
        String paymentMethodId,
        String currency,
        BigDecimal shippingFee,
        ShippingAddress shippingAddressSnapshot) implements Command {

    public record Line(String skuId, int qty) {
    }
}
