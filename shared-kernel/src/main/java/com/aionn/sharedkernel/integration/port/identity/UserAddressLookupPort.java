package com.aionn.sharedkernel.integration.port.identity;

import java.util.Optional;

/**
 * Cross-service port for fetching a user-owned shipping address by id. Used
 * by ordering and UCP during checkout when they need to snapshot the address
 * onto an order. Identity owns the address book, so it is the only service
 * that may read it directly.
 *
 * <p>The returned record is intentionally flat — no domain entities cross
 * the boundary.
 */
public interface UserAddressLookupPort {

    Optional<UserAddress> findOwned(String addressId, String userId);

    record UserAddress(
            String addressId,
            String contactName,
            String phone,
            String detailAddress,
            String wardCode,
            String wardName,
            String districtCode,
            String districtName,
            String provinceCode,
            String provinceName,
            String countryCode) {
    }
}
