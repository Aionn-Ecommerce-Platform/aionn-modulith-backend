package com.aionn.sharedkernel.integration.port.shipping;

import java.math.BigDecimal;

public interface ShippingFulfillmentPort {

        QuoteResult quote(String orderId, String merchantId, Address address, String currency);

        RegistrationResult createAndRegister(
                        String orderId,
                        String merchantId,
                        String userId,
                        Address address,
                        BigDecimal codAmount,
                        BigDecimal shippingFee,
                        String currency);

        record Address(
                        String fullName,
                        String phone,
                        String addressLine,
                        String wardCode,
                        String districtCode,
                        String provinceCode,
                        String countryCode) {
        }

        record QuoteResult(BigDecimal fee, String currency) {
        }

        record RegistrationResult(
                        String shipmentId,
                        String trackingCode,
                        String carrierOrderId,
                        String labelUrl) {
        }
}
