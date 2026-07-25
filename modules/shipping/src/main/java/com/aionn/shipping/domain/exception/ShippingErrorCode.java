package com.aionn.shipping.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ShippingErrorCode {
    SHIPMENT_NOT_FOUND("SHP_001", "Shipment not found"),
    SHIPMENT_INVALID_STATE("SHP_002", "Shipment is not in a state that allows this action"),
    SHIPMENT_ALREADY_PICKED_UP("SHP_003", "Shipment has already been picked up"),
    SHIPMENT_CARRIER_ERROR("SHP_004", "Carrier returned an error"),
    SHIPMENT_FORBIDDEN("SHP_005", "Shipment is not owned by the caller"),

    RATE_NOT_FOUND("SHP_101", "Shipping rate not found for the given zone"),
    RATE_DUPLICATE("SHP_102", "Shipping rate already configured for that zone"),

    INVALID_ARGUMENT("SHP_900", "Invalid argument");

    private final String code;
    private final String defaultMessage;
}
