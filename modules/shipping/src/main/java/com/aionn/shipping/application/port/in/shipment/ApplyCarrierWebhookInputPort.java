package com.aionn.shipping.application.port.in.shipment;

import com.aionn.shipping.application.dto.shipment.command.CarrierWebhookCommand;
import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;

public interface ApplyCarrierWebhookInputPort {
    ShipmentResult execute(CarrierWebhookCommand command);
}
