package com.aionn.shipping.infrastructure.integration;

import com.aionn.shipping.application.dto.rate.result.ShippingQuoteResult;
import com.aionn.shipping.application.dto.shipment.command.CreateShipmentCommand;
import com.aionn.shipping.application.dto.shipment.command.QuoteShippingCommand;
import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;
import com.aionn.shipping.application.port.in.shipment.RegisterShipmentInputPort;
import com.aionn.shipping.application.service.ShipmentService;
import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.shipping.domain.valueobject.ShipmentDimensions;
import com.aionn.shipping.infrastructure.config.ShippingProperties;
import com.aionn.sharedkernel.integration.port.shipping.ShippingFulfillmentPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("shippingFulfillmentPortAdapter")
@RequiredArgsConstructor
public class ShippingFulfillmentAdapter implements ShippingFulfillmentPort {

    private final ShipmentService shipmentService;
    private final RegisterShipmentInputPort registerShipmentInputPort;
    private final ShippingProperties properties;

    @Override
    public QuoteResult quote(String orderId, String merchantId, Address address, String currency) {
        ShippingQuoteResult result = shipmentService.quote(new QuoteShippingCommand(
                orderId, toShipmentAddress(address), defaultDimensions(), currency));
        return new QuoteResult(result.fee(), result.currency());
    }

    @Override
    public RegistrationResult createAndRegister(String orderId, String merchantId, String userId,
            Address address, java.math.BigDecimal codAmount, java.math.BigDecimal shippingFee, String currency) {
        CreateShipmentCommand command = new CreateShipmentCommand(
                orderId, merchantId, null, userId, toShipmentAddress(address), defaultDimensions(),
                codAmount, shippingFee, currency == null ? "VND" : currency);

        com.aionn.shipping.domain.model.Shipment created = shipmentService.createShipment(command);
        ShipmentResult shipment;
        try {
            shipment = registerShipmentInputPort.execute(created.getShipmentId());
        } catch (RuntimeException ex) {
            try {
                shipmentService.applyCancel(created.getShipmentId(), "carrier-registration-failed");
            } catch (RuntimeException compensateEx) {
                log.error("Failed to compensate orphan shipment {} after carrier registration failed",
                        created.getShipmentId(), compensateEx);
            }
            throw ex;
        }

        return new RegistrationResult(shipment.shipmentId(), shipment.trackingCode(),
                shipment.carrierOrderId(), shipment.labelUrl());
    }

    private ShipmentDimensions defaultDimensions() {
        ShippingProperties.DefaultDimensions d = properties.defaultDimensions();
        return new ShipmentDimensions(d.weightGram(), d.lengthCm(), d.widthCm(), d.heightCm());
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
