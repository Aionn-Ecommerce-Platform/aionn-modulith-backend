package com.aionn.identity.infrastructure.persistence.adapter.address;

import com.aionn.identity.application.port.out.address.AddressPersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.Address;
import com.aionn.identity.infrastructure.persistence.entity.UserAddressEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.mapper.AddressDomainMapper;
import com.aionn.identity.infrastructure.persistence.repository.address.AddressRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AddressPersistenceAdapter implements AddressPersistencePort {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressDomainMapper addressDomainMapper;

    @Override
    public List<Address> findByUserId(String userId) {
        return addressRepository.findByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(addressDomainMapper::toDomain)
                .toList();
    }

    @Override
    public long countByUserId(String userId) {
        return addressRepository.countByUser_UserId(userId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Address createAtomically(Address address, long maxAddressNumbers, boolean makeDefaultWhenFirst) {
        long currentCount = addressRepository.countByUser_UserId(address.userId());
        if (currentCount >= maxAddressNumbers) {
            throw new IdentityException(IdentityErrorCode.ADDRESS_NUMBER_EXCEEDED);
        }

        boolean shouldBeDefault = address.isDefault() || (makeDefaultWhenFirst && currentCount == 0);
        if (shouldBeDefault) {
            addressRepository.clearDefaultAddressByUserId(address.userId());
        }

        Address addressToPersist = shouldBeDefault && !address.isDefault()
                ? new Address(
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
                        address.updatedAt())
                : address;

        UserEntity userEntity = userRepository.getReferenceById(addressToPersist.userId());
        UserAddressEntity entity = addressDomainMapper.toEntity(addressToPersist, userEntity);
        UserAddressEntity saved = addressRepository.save(entity);
        return addressDomainMapper.toDomain(saved);
    }

    @Override
    public Address save(Address address) {
        UserEntity userEntity = userRepository.getReferenceById(address.userId());
        var entity = addressDomainMapper.toEntity(address, userEntity);
        var savedEntity = addressRepository.save(entity);
        return addressDomainMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Address> findByAddressIdAndUserId(String addressId, String userId) {
        return addressRepository.findByAddressIdAndUser_UserId(addressId, userId)
                .map(addressDomainMapper::toDomain);
    }

    @Override
    public void clearDefaultByUserId(String userId) {
        addressRepository.clearDefaultAddressByUserId(userId);
    }

    @Override
    public void delete(Address address) {
        addressRepository.deleteById(address.addressId());
    }
}
