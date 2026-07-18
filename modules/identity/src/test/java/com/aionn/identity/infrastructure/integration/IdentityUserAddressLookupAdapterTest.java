package com.aionn.identity.infrastructure.integration;
import com.aionn.identity.infrastructure.integration.address.IdentityUserAddressLookupAdapter;

import com.aionn.identity.application.port.out.address.AddressPersistencePort;
import com.aionn.identity.domain.model.Address;
import com.aionn.identity.domain.valueobject.AddressType;
import com.aionn.sharedkernel.integration.port.identity.UserAddressLookupPort.UserAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityUserAddressLookupAdapterTest {

    @Mock
    private AddressPersistencePort addressPersistencePort;

    private IdentityUserAddressLookupAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new IdentityUserAddressLookupAdapter(addressPersistencePort);
    }

    @Test
    void returnsEmptyWhenAddressIdNull() {
        assertThat(adapter.findOwned(null, "user-1")).isEmpty();
    }

    @Test
    void returnsEmptyWhenUserIdNull() {
        assertThat(adapter.findOwned("addr-1", null)).isEmpty();
    }

    @Test
    void returnsEmptyWhenNotFound() {
        when(addressPersistencePort.findByAddressIdAndUserId("addr-1", "user-1"))
                .thenReturn(Optional.empty());

        assertThat(adapter.findOwned("addr-1", "user-1")).isEmpty();
    }

    @Test
    void mapsAddressToUserAddressView() {
        Address address = new Address(
                "addr-1", "user-1", "Contact", "0900000000",
                "p1", "Province", "d1", "District", "w1", "Ward",
                "12 Some Street", "12 Some Street, Ward, District, Province",
                AddressType.HOME, true,
                Instant.now(), Instant.now());
        when(addressPersistencePort.findByAddressIdAndUserId("addr-1", "user-1"))
                .thenReturn(Optional.of(address));

        Optional<UserAddress> result = adapter.findOwned("addr-1", "user-1");

        assertThat(result).isPresent();
        UserAddress view = result.get();
        assertThat(view.addressId()).isEqualTo("addr-1");
        assertThat(view.contactName()).isEqualTo("Contact");
        assertThat(view.phone()).isEqualTo("0900000000");
        assertThat(view.detailAddress()).isEqualTo("12 Some Street");
        assertThat(view.wardCode()).isEqualTo("w1");
        assertThat(view.wardName()).isEqualTo("Ward");
        assertThat(view.districtCode()).isEqualTo("d1");
        assertThat(view.districtName()).isEqualTo("District");
        assertThat(view.provinceCode()).isEqualTo("p1");
        assertThat(view.provinceName()).isEqualTo("Province");
        assertThat(view.countryCode()).isEqualTo("VN");
    }
}
