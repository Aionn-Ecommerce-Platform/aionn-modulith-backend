package com.aionn.identity.infrastructure.persistence.mapper;

import com.aionn.identity.application.dto.address.command.UpdateAddressCommand;
import com.aionn.identity.application.dto.address.command.CreateAddressCommand;
import com.aionn.identity.domain.model.Address;
import com.aionn.identity.domain.valueobject.AddressType;
import com.aionn.identity.infrastructure.persistence.entity.UserAddressEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AddressDomainMapper {

	@Mapping(target = "userId", source = "user.userId")
	Address toDomain(UserAddressEntity entity);

	@Mapping(target = "user", source = "userEntity")
	@Mapping(target = "phone", source = "address.phone")
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	UserAddressEntity toEntity(Address address, UserEntity userEntity);

	@Mapping(target = "addressId", expression = "java(com.aionn.sharedkernel.util.IdGenerator.ulid())")
	@Mapping(target = "user", source = "userEntity")
	@Mapping(target = "phone", source = "command.phone")
	@Mapping(target = "isDefault", source = "isDefault")
	@Mapping(target = "fullAddress", source = "fullAddress")
	@Mapping(target = "provinceName", source = "provinceName")
	@Mapping(target = "districtName", source = "districtName")
	@Mapping(target = "wardName", source = "wardName")
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	UserAddressEntity toEntity(CreateAddressCommand command, UserEntity userEntity, boolean isDefault,
			String fullAddress, String provinceName, String districtName, String wardName);

	@Mapping(target = "addressId", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "isDefault", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	void updateEntityFromCommand(UpdateAddressCommand command, @MappingTarget UserAddressEntity entity,
			String fullAddress, String provinceName, String districtName, String wardName);

	default AddressType mapType(String value) {
		return value == null ? null : AddressType.valueOf(value);
	}

	default String mapType(AddressType value) {
		return value == null ? null : value.name();
	}
}

