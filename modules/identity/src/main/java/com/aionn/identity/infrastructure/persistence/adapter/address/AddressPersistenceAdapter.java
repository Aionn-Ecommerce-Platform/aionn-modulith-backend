package com.aionn.identity.infrastructure.persistence.adapter.address;

import com.aionn.identity.application.port.out.address.AddressPersistencePort;
import com.aionn.identity.domain.model.Address;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.mapper.AddressDomainMapper;
import com.aionn.identity.infrastructure.persistence.repository.address.AddressRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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

