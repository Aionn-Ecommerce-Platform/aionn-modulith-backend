package com.aionn.identity.adapter.rest.dto.address.response;

import java.time.Instant;

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
		String fullAddress,
		String type,
		boolean isDefault,
		Instant createdAt,
		Instant updatedAt) {
}
