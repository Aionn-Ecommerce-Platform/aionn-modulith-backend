package com.aionn.shipping.domain.valueobject;

public record ShipmentAddress(
        String fullName,
        String phone,
        String addressLine,
        String wardCode,
        String districtId,
        String provinceCode,
        String countryCode) {
}

