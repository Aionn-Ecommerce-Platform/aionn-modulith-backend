package com.aionn.ordering.infrastructure.integration.shipping;

import com.aionn.ordering.application.port.out.ShippingGateway;
import com.aionn.ordering.domain.valueobject.ShippingAddress;
import com.aionn.contracts.port.shipping.ShippingFulfillmentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ShippingFulfillmentAdapter implements ShippingGateway {

    private final ShippingFulfillmentPort shippingFulfillmentPort;

    @Override
    public ShippingQuote quote(String orderId, String merchantId, ShippingAddress address, String currency,
            List<Parcel> parcels, BigDecimal goodsSubtotal) {
        ShippingFulfillmentPort.QuoteResult result = shippingFulfillmentPort.quote(
                orderId, merchantId, toPortAddress(address), currency, toPortParcels(parcels), goodsSubtotal);
        return new ShippingQuote(result.fee(), result.currency());
    }

    @Override
    public ShippingQuote quote(String orderId, String merchantId, ShippingAddress address, String currency,
            List<Parcel> parcels, BigDecimal goodsSubtotal, String serviceCode) {
        if (serviceCode == null || serviceCode.isBlank() || "STANDARD".equalsIgnoreCase(serviceCode)) {
            return quote(orderId, merchantId, address, currency, parcels, goodsSubtotal);
        }
        ShippingFulfillmentPort.QuoteResult result = shippingFulfillmentPort.quote(
                orderId, merchantId, toPortAddress(address), currency, toPortParcels(parcels), goodsSubtotal,
                serviceCode);
        return new ShippingQuote(result.fee(), result.currency());
    }

    @Override
    public List<Registration> createAndRegister(String orderId, String merchantId, String userId,
            ShippingAddress address, BigDecimal codAmount, BigDecimal shippingFee, String currency,
            List<Parcel> parcels) {
        return shippingFulfillmentPort.createAndRegister(
                        orderId, merchantId, userId, toPortAddress(address), codAmount, shippingFee, currency,
                        toPortParcels(parcels)).stream()
                .map(r -> new Registration(r.shipmentId(), r.trackingCode(), r.carrierOrderId(), r.labelUrl()))
                .toList();
    }

    private List<ShippingFulfillmentPort.Parcel> toPortParcels(List<Parcel> parcels) {
        if (parcels == null) {
            return List.of();
        }
        return parcels.stream()
                .map(p -> new ShippingFulfillmentPort.Parcel(
                        p.weightGram(), p.lengthCm(), p.widthCm(), p.heightCm(), p.qty()))
                .toList();
    }

    private ShippingFulfillmentPort.Address toPortAddress(ShippingAddress address) {
        if (address == null) {
            return null;
        }
        return new ShippingFulfillmentPort.Address(address.fullName(), address.phone(),
                address.addressLine(), address.wardCode(), address.districtCode(),
                address.provinceCode(), address.countryCode());
    }
}
