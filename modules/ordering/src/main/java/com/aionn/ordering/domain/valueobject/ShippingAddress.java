package com.aionn.ordering.domain.valueobject;

/**
 * Snapshot of the shipping address at the moment the order is placed.
 * Stored on the order so a user editing their address book later does not
 * change historical orders.
 */
public record ShippingAddress(
        String addressId,
        String fullName,
        String phone,
        String addressLine,
        String wardCode,
        String districtCode,
        String provinceCode,
        String countryCode) {
}

