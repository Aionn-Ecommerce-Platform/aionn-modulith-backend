package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.AddressType;

import java.time.Instant;

public record Address(
		String addressId,
		String userId,
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
		AddressType type,
		boolean isDefault,
		Instant createdAt,
		Instant updatedAt) {

	public boolean canBeDeleted() {
		return !isDefault;
	}
}



