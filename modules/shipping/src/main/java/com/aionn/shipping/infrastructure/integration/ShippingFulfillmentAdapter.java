package com.aionn.shipping.infrastructure.integration;

import com.aionn.shipping.application.dto.rate.result.ShippingQuoteResult;
import com.aionn.shipping.application.dto.shipment.command.CreateShipmentCommand;
import com.aionn.shipping.application.dto.shipment.command.QuoteShippingCommand;
import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;
import com.aionn.shipping.application.port.in.shipment.RegisterShipmentInputPort;
import com.aionn.shipping.application.service.ShipmentService;
import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.sharedkernel.integration.port.shipping.ShippingFulfillmentPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component("shippingFulfillmentPortAdapter")
@RequiredArgsConstructor
public class ShippingFulfillmentAdapter implements ShippingFulfillmentPort {

    private final ShipmentService shipmentService;
    private final RegisterShipmentInputPort registerShipmentInputPort;

    @Override
    public QuoteResult quote(String orderId, String merchantId, Address address, String currency,
            List<Parcel> parcels, java.math.BigDecimal goodsSubtotal) {
        ShippingQuoteResult result = shipmentService.quote(new QuoteShippingCommand(
                orderId, toShipmentAddress(address), toCommandParcels(parcels), goodsSubtotal, currency));
        return new QuoteResult(result.fee(), result.currency());
    }

    @Override
    public List<RegistrationResult> createAndRegister(String orderId, String merchantId, String userId,
            Address address, java.math.BigDecimal codAmount, java.math.BigDecimal shippingFee,
            String currency, List<Parcel> parcels) {
        CreateShipmentCommand command = new CreateShipmentCommand(
                orderId, merchantId, null, userId, toShipmentAddress(address),
                toCommandParcels(parcels),
                codAmount, shippingFee, currency == null ? "VND" : currency);

        List<com.aionn.shipping.domain.model.Shipment> created =
                shipmentService.createShipmentsPerConsignment(command);
        List<RegistrationResult> results = new java.util.ArrayList<>(created.size());
        for (com.aionn.shipping.domain.model.Shipment shipment : created) {
            try {
                // Each physical parcel has its own carrier tracking code/label.
                ShipmentResult registered = registerShipmentInputPort.executeByMerchant(
                        shipment.getShipmentId(), merchantId);
                results.add(new RegistrationResult(registered.shipmentId(), registered.trackingCode(),
                        registered.carrierOrderId(), registered.labelUrl()));
            } catch (RuntimeException ex) {
                try {
                    shipmentService.applyCancel(shipment.getShipmentId(), "carrier-registration-failed");
                } catch (RuntimeException compensateEx) {
                    log.error("Failed to compensate orphan shipment {} after carrier registration failed",
                            shipment.getShipmentId(), compensateEx);
                }
                throw ex;
            }
        }
        return List.copyOf(results);
    }

    private List<QuoteShippingCommand.Parcel> toCommandParcels(List<Parcel> parcels) {
        if (parcels == null) {
            return List.of();
        }
        return parcels.stream()
                .map(p -> new QuoteShippingCommand.Parcel(
                        p.weightGram(), p.lengthCm(), p.widthCm(), p.heightCm(), p.qty()))
                .toList();
    }

    private ShipmentAddress toShipmentAddress(Address addr) {
        if (addr == null) {
            return null;
        }
        return new ShipmentAddress(
                addr.fullName(),
                addr.phone(),
                addr.addressLine(),
                addr.wardCode(),
                addr.districtCode(),
                addr.provinceCode(),
                addr.countryCode());
    }
}
