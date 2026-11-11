package com.aionn.shipping.infrastructure.integration;

import com.aionn.shipping.domain.model.Shipment;
import java.math.BigDecimal;
import java.util.ArrayList;

import com.aionn.shipping.application.dto.rate.result.ShippingQuoteResult;
import com.aionn.shipping.application.dto.shipment.command.CreateShipmentCommand;
import com.aionn.shipping.application.dto.shipment.command.QuoteShippingCommand;
import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;
import com.aionn.shipping.application.port.in.shipment.RegisterShipmentInputPort;
import com.aionn.shipping.application.service.ShipmentCarrierOrchestrator;
import com.aionn.shipping.application.service.ShipmentService;
import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.contracts.port.shipping.ShippingFulfillmentPort;
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
    private final ShipmentCarrierOrchestrator shipmentCarrierOrchestrator;

    @Override
    public QuoteResult quote(String orderId, String merchantId, Address address, String currency,
            List<Parcel> parcels, BigDecimal goodsSubtotal) {
        return quote(orderId, merchantId, address, currency, parcels, goodsSubtotal, "STANDARD");
    }

    @Override
    public QuoteResult quote(String orderId, String merchantId, Address address, String currency,
            List<Parcel> parcels, BigDecimal goodsSubtotal, String serviceCode) {
        ShippingQuoteResult result = shipmentService.quote(new QuoteShippingCommand(
                orderId, toShipmentAddress(address), toCommandParcels(parcels), goodsSubtotal, currency,
                serviceCode));
        return new QuoteResult(result.fee(), result.currency());
    }

    @Override
    public List<RegistrationResult> createAndRegister(String orderId, String merchantId, String userId,
            Address address, BigDecimal codAmount, BigDecimal shippingFee,
            String currency, List<Parcel> parcels) {
        CreateShipmentCommand command = new CreateShipmentCommand(
                orderId, merchantId, null, userId, toShipmentAddress(address),
                toCommandParcels(parcels),
                codAmount, shippingFee, currency);

        List<Shipment> created =
                shipmentService.createShipmentsPerConsignment(command);
        List<RegistrationResult> results = new ArrayList<>(created.size());
        for (Shipment shipment : created) {
            try {
                // Each physical parcel has its own carrier tracking code/label.
                ShipmentResult registered = registerShipmentInputPort.executeByMerchant(
                        shipment.getShipmentId(), merchantId);
                results.add(new RegistrationResult(registered.shipmentId(), registered.trackingCode(),
                        registered.carrierOrderId(), registered.labelUrl()));
            } catch (RuntimeException ex) {
                compensate(created, ex);
                throw ex;
            }
        }
        return List.copyOf(results);
    }

    /**
     * A multi-consignment order is all-or-nothing: when one parcel fails to register,
     * cancel every parcel already handed to the carrier, not just the failing one.
     */
    private void compensate(List<Shipment> created, RuntimeException cause) {
        for (Shipment orphan : created) {
            try {
                Shipment latest = shipmentService.loadShipment(orphan.getShipmentId());
                if (latest.getTrackingCode() != null) {
                    shipmentCarrierOrchestrator.cancelShipmentSystem(
                            latest.getShipmentId(), "carrier-registration-failed");
                } else if (latest.isCancellable()) {
                    shipmentService.applyCancel(latest.getShipmentId(), "carrier-registration-failed");
                }
            } catch (RuntimeException compensateEx) {
                log.error("Failed to compensate shipment {} after carrier registration failed",
                        orphan.getShipmentId(), compensateEx);
                cause.addSuppressed(compensateEx);
            }
        }
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
