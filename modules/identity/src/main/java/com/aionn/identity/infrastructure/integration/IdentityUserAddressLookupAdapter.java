package com.aionn.identity.infrastructure.integration;

import com.aionn.identity.application.port.out.address.AddressPersistencePort;
import com.aionn.identity.domain.model.Address;
import com.aionn.sharedkernel.integration.port.identity.UserAddressLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Identity-side adapter for the cross-service user-address port. Strips
 * domain types and exposes only the fields downstream services need to snapshot
 * an address onto an order.
 */
@Component
@RequiredArgsConstructor
public class IdentityUserAddressLookupAdapter implements UserAddressLookupPort {

    private final AddressPersistencePort addressPersistencePort;

    @Override
    public Optional<UserAddress> findOwned(String addressId, String userId) {
        if (addressId == null || userId == null) {
            return Optional.empty();
        }
        return addressPersistencePort.findByAddressIdAndUserId(addressId, userId)
                .map(IdentityUserAddressLookupAdapter::toView);
    }

    private static UserAddress toView(Address a) {
        return new UserAddress(
                a.addressId(),
                a.contactName(),
                a.phone(),
                a.detailAddress(),
                a.wardCode(),
                a.wardName(),
                a.districtCode(),
                a.districtName(),
                a.provinceCode(),
                a.provinceName(),
                "VN");
    }
}
