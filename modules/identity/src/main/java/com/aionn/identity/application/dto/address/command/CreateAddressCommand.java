package com.aionn.identity.application.dto.address.command;

import com.aionn.identity.domain.valueobject.AddressType;
import com.aionn.sharedkernel.application.command.Command;

public record CreateAddressCommand(
		String userId,
		String contactName,
		String phone,
		String provinceCode,
		String districtCode,
		String wardCode,
		String detailAddress,
		AddressType type,
		boolean isDefault) implements Command {
}



