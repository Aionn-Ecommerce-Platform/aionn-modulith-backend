package com.aionn.identity.application.service;

import com.aionn.identity.application.dto.address.command.CreateAddressCommand;
import com.aionn.identity.application.dto.address.command.UpdateAddressCommand;
import com.aionn.identity.application.dto.geography.result.ResolvedLocation;
import com.aionn.identity.application.port.out.address.AddressPersistencePort;
import com.aionn.identity.application.policy.AddressPolicy;
import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.Address;
import com.aionn.sharedkernel.domain.vo.PhoneNumber;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AddressService {

	private final AddressPersistencePort addressPersistencePort;
	private final UserPersistencePort userPersistencePort;
	private final AddressPolicy addressPolicy;
	private final GeographyService geographyService;

	@Transactional(readOnly = true)
	public List<Address> listAddressesByUserId(String userId) {
		return addressPersistencePort.findByUserId(userId);
	}

	public Address createAddress(CreateAddressCommand command) {
		log.info("Creating address for user: {}", command.userId());
		validatePhone(command.phone());

		if (!userPersistencePort.existsById(command.userId())) {
			throw new IdentityException(IdentityErrorCode.USER_NOT_FOUND);
		}

		long count = addressPersistencePort.countByUserId(command.userId());
		if (count >= addressPolicy.getMaxAddressNumbers()) {
			throw new IdentityException(IdentityErrorCode.ADDRESS_NUMBER_EXCEEDED);
		}

		ResolvedLocation location = geographyService.resolveLocation(
				command.provinceCode(), command.districtCode(), command.wardCode());

		String fullAddress = location.buildFullAddress(command.detailAddress());
		boolean shouldSetAsDefault = (count == 0) || command.isDefault();
		if (shouldSetAsDefault) {
			addressPersistencePort.clearDefaultByUserId(command.userId());
		}

		LocalDateTime now = LocalDateTime.now();
		Address address = new Address(
				IdGenerator.ulid(),
				command.userId(),
				command.contactName(),
				command.phone(),
				command.provinceCode(),
				location.province().name(),
				command.districtCode(),
				location.district().name(),
				command.wardCode(),
				location.ward().name(),
				command.detailAddress(),
				fullAddress,
				command.type(),
				shouldSetAsDefault,
				now,
				now);

		return addressPersistencePort.save(address);
	}

	public Address updateAddress(UpdateAddressCommand command) {
		log.info("Updating address: {} for user: {}", command.addressId(), command.userId());
		validatePhone(command.phone());

		Address address = getAddressOrThrow(command.userId(), command.addressId());

		String fullAddress = address.fullAddress();
		String provinceName = address.provinceName();
		String districtName = address.districtName();
		String wardName = address.wardName();

		boolean locationChanged = !command.provinceCode().equals(address.provinceCode())
				|| !command.districtCode().equals(address.districtCode())
				|| !command.wardCode().equals(address.wardCode())
				|| !command.detailAddress().equals(address.detailAddress());

		if (locationChanged) {
			ResolvedLocation location = geographyService.resolveLocation(
					command.provinceCode(), command.districtCode(), command.wardCode());
			fullAddress = location.buildFullAddress(command.detailAddress());
			provinceName = location.province().name();
			districtName = location.district().name();
			wardName = location.ward().name();
		}

		Address updatedAddress = new Address(
				address.addressId(),
				address.userId(),
				command.contactName(),
				command.phone(),
				command.provinceCode(),
				provinceName,
				command.districtCode(),
				districtName,
				command.wardCode(),
				wardName,
				command.detailAddress(),
				fullAddress,
				command.type(),
				address.isDefault(),
				address.createdAt(),
				LocalDateTime.now());

		return addressPersistencePort.save(updatedAddress);
	}

	public void deleteAddress(String userId, String addressId) {
		Address address = getAddressOrThrow(userId, addressId);
		if (!address.canBeDeleted()) {
			throw new IdentityException(IdentityErrorCode.DEFAULT_ADDRESS_CANNOT_BE_DELETED);
		}
		addressPersistencePort.delete(address);
	}

	public Address setDefaultAddress(String userId, String addressId) {
		Address address = getAddressOrThrow(userId, addressId);
		if (address.isDefault()) {
			return address;
		}
		addressPersistencePort.clearDefaultByUserId(userId);

		Address updatedAddress = new Address(
				address.addressId(),
				address.userId(),
				address.contactName(),
				address.phone(),
				address.provinceCode(),
				address.provinceName(),
				address.districtCode(),
				address.districtName(),
				address.wardCode(),
				address.wardName(),
				address.detailAddress(),
				address.fullAddress(),
				address.type(),
				true,
				address.createdAt(),
				LocalDateTime.now());

		return addressPersistencePort.save(updatedAddress);
	}

	private static void validatePhone(String phone) {
		try {
			PhoneNumber.of(phone);
		} catch (IllegalArgumentException ex) {
			throw new IdentityException(IdentityErrorCode.PHONE_INVALID, ex.getMessage());
		}
	}

	private Address getAddressOrThrow(String userId, String addressId) {
		return addressPersistencePort.findByAddressIdAndUserId(addressId, userId)
				.orElseThrow(() -> new IdentityException(IdentityErrorCode.ADDRESS_NOT_FOUND, "Address not found"));
	}
}
