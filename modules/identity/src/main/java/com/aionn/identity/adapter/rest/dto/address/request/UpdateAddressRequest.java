package com.aionn.identity.adapter.rest.dto.address.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAddressRequest(
		@NotBlank(message = "Recipient name is required") @Size(max = 100, message = "Recipient name must be at most 100 characters") String contactName,

		@NotBlank(message = "Phone number is required") @Size(max = 20, message = "Phone number must be at most 20 characters") String phone,

		@NotBlank(message = "Province code is required") String provinceCode,

		@NotBlank(message = "District code is required") String districtCode,

		@NotBlank(message = "Ward code is required") String wardCode,

		@NotBlank(message = "Detail address is required") @Size(max = 500, message = "Detail address must be at most 500 characters") String detailAddress,

		@NotBlank(message = "Address type is required") @Pattern(regexp = "HOME|OFFICE", message = "Address type must be HOME or OFFICE") String type) {
}
