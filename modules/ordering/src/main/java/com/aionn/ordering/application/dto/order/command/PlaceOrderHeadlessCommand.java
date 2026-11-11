package com.aionn.ordering.application.dto.order.command;

import com.aionn.ordering.domain.valueobject.ShippingAddress;
import com.aionn.sharedkernel.application.command.Command;

import java.util.List;

/**
 * Place an order without going through the user's cart �?" used by headless
 * channels such as the UCP agentic checkout where the line items come
 * directly in the request payload.
 *
 * @param lines       list of {sku, qty} pairs the agent is committing to.
 * @param voucherCode optional voucher to apply (null = none).
 * @param note        optional delivery note supplied by the buyer.
 * @param shippingService optional service level (STANDARD / EXPRESS).
 */
public record PlaceOrderHeadlessCommand(
        String userId,
        List<Line> lines,
        String voucherCode,
        String paymentMethodId,
        String currency,
        ShippingAddress shippingAddressSnapshot,
        String idempotencyKey,
        String note,
        String shippingService) implements Command {

    public PlaceOrderHeadlessCommand(String userId, List<Line> lines, String voucherCode,
            String paymentMethodId, String currency, ShippingAddress shippingAddressSnapshot) {
        this(userId, lines, voucherCode, paymentMethodId, currency, shippingAddressSnapshot, null, null, null);
    }

    public PlaceOrderHeadlessCommand(String userId, List<Line> lines, String voucherCode,
            String paymentMethodId, String currency, ShippingAddress shippingAddressSnapshot,
            String idempotencyKey) {
        this(userId, lines, voucherCode, paymentMethodId, currency, shippingAddressSnapshot,
                idempotencyKey, null, null);
    }

    public PlaceOrderHeadlessCommand(String userId, List<Line> lines, String voucherCode,
            String paymentMethodId, String currency, ShippingAddress shippingAddressSnapshot,
            String idempotencyKey, String note) {
        this(userId, lines, voucherCode, paymentMethodId, currency, shippingAddressSnapshot,
                idempotencyKey, note, null);
    }

    public record Line(String skuId, int qty) {
    }
}
