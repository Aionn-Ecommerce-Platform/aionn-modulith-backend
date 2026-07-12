package com.aionn.identity.application.dto.address.result;

import com.aionn.identity.domain.valueobject.AddressType;

import java.time.Instant;

public record AddressResult(
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
}


