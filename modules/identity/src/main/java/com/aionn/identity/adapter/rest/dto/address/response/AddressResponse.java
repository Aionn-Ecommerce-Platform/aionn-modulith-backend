package com.aionn.identity.adapter.rest.dto.address.response;

import com.aionn.identity.domain.valueobject.AddressType;

import java.time.LocalDateTime;

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
		AddressType type,
		boolean isDefault,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {
}

