package com.aionn.sharedkernel.integration.port.ordering;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cross-service order placement port. Used by callers that already know the
 * exact line items they want to commit (UCP agentic checkout, scheduled jobs,
 * imports). Cart-driven placement stays internal to ordering.
 *
 * <p>Address fields are flat — ordering's domain ShippingAddress value object
 * does not cross the boundary; the implementation is responsible for building
 * its own snapshot from the raw fields.
 */
public interface OrderPlacementPort {

    PlacedOrder placeHeadless(PlaceCommand command);

    record PlaceCommand(
            String userId,
            List<Line> lines,
            String voucherCode,
            String paymentMethodId,
            String currency,
            BigDecimal shippingFee,
            ShippingAddress shippingAddress) {

        public record Line(String skuId, int qty) {
        }

        public record ShippingAddress(
                String addressId,
                String contactName,
                String phone,
                String detailAddress,
                String wardCode,
                String districtCode,
                String provinceCode,
                String countryCode) {
        }
    }

    record PlacedOrder(String orderId, long totalAmountMinor, String currency, String status) {
    }
}
