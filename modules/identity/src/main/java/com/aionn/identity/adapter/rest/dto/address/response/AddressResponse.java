package com.aionn.identity.adapter.rest.dto.address.response;

import java.time.Instant;

// AddressType exposed as a String (enum name) rather than the domain enum type,
// so the REST contract doesn't move if the domain enum is renamed / reordered.
public record AddressResponse(
		String addressId,
		String contactName,
		String phone,
		String provinceCode,
		String provinceName,
		String districtCode,
		String districtName,
		String wardCode,
		String wardName,
		String detailAddress,
		String fullAddress, // Formatted string: "123 Street, Ward Y, District Z, City A"
		String type,
		boolean isDefault,
		Instant createdAt,
		Instant updatedAt) {
}
