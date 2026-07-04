package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.address.request.CreateAddressRequest;
import com.aionn.identity.adapter.rest.dto.address.request.UpdateAddressRequest;
import com.aionn.identity.adapter.rest.dto.address.response.AddressResponse;
import com.aionn.identity.adapter.rest.mapper.address.AddressDtoMapper;
import com.aionn.identity.application.port.in.address.*;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Identity - Address Book", description = "Identity module: delivery address management endpoints")
public class AddressController {

	private final ListAddressesQueryPort listAddressesQueryPort;
	private final CreateAddressInputPort createAddressInputPort;
	private final UpdateAddressInputPort updateAddressInputPort;
	private final DeleteAddressInputPort deleteAddressInputPort;
	private final SetDefaultAddressInputPort setDefaultAddressInputPort;
	private final AddressDtoMapper addressDtoMapper;

	@GetMapping
	@Operation(summary = "List user addresses", description = "Get all delivery addresses for the authenticated user")
	public ResponseEntity<ApiResponse<List<AddressResponse>>> list(Authentication authentication) {
		var result = listAddressesQueryPort.execute(authentication.getName());
		var response = addressDtoMapper.toResponses(result);
		return ResponseEntity.ok(ApiResponse.success(response, "Addresses fetched"));
	}

	@PostMapping
	@Operation(summary = "Create address", description = "Create a new delivery address for the authenticated user")
	public ResponseEntity<ApiResponse<AddressResponse>> create(
			Authentication authentication,
			@Valid @RequestBody CreateAddressRequest request) {
		var result = createAddressInputPort.execute(addressDtoMapper.toCreateCommand(authentication.getName(), request));
		var response = addressDtoMapper.toResponse(result);
		return ApiResponse.createdResponse("Address created!", response);
	}

	@PutMapping("/{addressId}")
	@Operation(summary = "Update address", description = "Update an existing delivery address by address ID")
	public ResponseEntity<ApiResponse<AddressResponse>> update(
			Authentication authentication,
			@PathVariable String addressId,
			@Valid @RequestBody UpdateAddressRequest request) {
		var result = updateAddressInputPort
				.execute(addressDtoMapper.toUpdateCommand(authentication.getName(), addressId, request));
		var response = addressDtoMapper.toResponse(result);
		return ResponseEntity.ok(ApiResponse.success(response, "Address updated"));
	}

	@DeleteMapping("/{addressId}")
	@Operation(summary = "Delete address", description = "Delete a delivery address by address ID")
	public ResponseEntity<ApiResponse<Void>> delete(
			Authentication authentication,
			@PathVariable String addressId) {
		deleteAddressInputPort.execute(addressDtoMapper.toDeleteCommand(authentication.getName(), addressId));
		return ResponseEntity.ok(ApiResponse.success("Address deleted"));
	}

	@PatchMapping("/{addressId}/default")
	@Operation(summary = "Set default address", description = "Mark one delivery address as default for the authenticated user")
	public ResponseEntity<ApiResponse<AddressResponse>> setDefault(
			Authentication authentication,
			@PathVariable String addressId) {
		var result = setDefaultAddressInputPort
				.execute(addressDtoMapper.toSetDefaultCommand(authentication.getName(), addressId));
		var response = addressDtoMapper.toResponse(result);
		return ResponseEntity.ok(ApiResponse.success(response, "Default address updated"));
	}
}

