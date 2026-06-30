package com.aionn.sharedkernel.integration.port.shipping;

import java.math.BigDecimal;

/**
 * Outbound port for quoting shipping fees and creating shipments.
 *
 * <p>
 * Used synchronously by the Ordering module. Both methods must complete inline
 * so the order can be priced and shipment id can be persisted on the order.
 * </p>
 */
public interface ShippingGatewayPort {

    ShippingQuote quote(String orderId, String merchantId, ShippingAddress address, String currency);

    RegistrationResult createShipment(
            String orderId,
            String merchantId,
            String userId,
            ShippingAddress address,
            BigDecimal codAmount,
            BigDecimal shippingFee,
            String currency);

    /**
     * Snapshot of the recipient address used by Shipping.
     * Mirrors the structure of the Ordering domain {@code ShippingAddress} but
     * lives in shared-kernel so it can be referenced by both Ordering and
     * Shipping modules without leaking domain types.
     */
    record ShippingAddress(
            String addressId,
            String fullName,
            String phone,
            String addressLine,
            String wardCode,
            String districtCode,
            String provinceCode,
            String countryCode) {
    }

    record ShippingQuote(BigDecimal fee, String currency) {
    }

    record RegistrationResult(
            String shipmentId,
            String trackingCode,
            String carrierOrderId,
            String labelUrl) {
    }
}
