package com.aionn.identity.adapter.rest.mapper.address;

import com.aionn.identity.adapter.rest.dto.address.request.CreateAddressRequest;
import com.aionn.identity.adapter.rest.dto.address.request.UpdateAddressRequest;
import com.aionn.identity.adapter.rest.dto.address.response.AddressResponse;
import com.aionn.identity.application.dto.address.command.CreateAddressCommand;
import com.aionn.identity.application.dto.address.command.DeleteAddressCommand;
import com.aionn.identity.application.dto.address.command.SetDefaultAddressCommand;
import com.aionn.identity.application.dto.address.command.UpdateAddressCommand;
import com.aionn.identity.application.dto.address.result.AddressResult;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.valueobject.AddressType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AddressDtoMapper {

	// request -> command
	@Mapping(target = "type", expression = "java(mapStringToAddressType(request.type()))")
	CreateAddressCommand toCreateCommand(String userId, CreateAddressRequest request);

	@Mapping(target = "type", expression = "java(mapStringToAddressType(request.type()))")
	UpdateAddressCommand toUpdateCommand(String userId, String addressId, UpdateAddressRequest request);

	DeleteAddressCommand toDeleteCommand(String userId, String addressId);

	SetDefaultAddressCommand toSetDefaultCommand(String userId, String addressId);

	// result -> response
	AddressResponse toResponse(AddressResult result);

	List<AddressResponse> toResponses(List<AddressResult> results);

	// helper convert - validates and converts String to AddressType enum
	default AddressType mapStringToAddressType(String type) {
		if (type == null)
			return null;
		try {
			return AddressType.valueOf(type.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IdentityException(IdentityErrorCode.INVALID_ADDRESS_TYPE);
		}
	}
}
