package com.aionn.identity.application.service;

import com.aionn.identity.application.dto.address.command.CreateAddressCommand;
import com.aionn.identity.application.dto.address.command.UpdateAddressCommand;
import com.aionn.identity.application.dto.geography.result.GeographyResult;
import com.aionn.identity.application.dto.geography.result.ResolvedLocation;
import com.aionn.identity.application.policy.AddressPolicy;
import com.aionn.identity.application.port.out.address.AddressPersistencePort;
import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.Address;
import com.aionn.identity.domain.valueobject.AddressType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.Clock;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String ADDRESS_ID = "01HZADDR000000000000000000";

    @Mock
    private AddressPersistencePort addressPersistencePort;
    @Mock
    private UserPersistencePort userPersistencePort;
    @Mock
    private AddressPolicy addressPolicy;
    @Mock
    private GeographyService geographyService;

    private AddressService addressService;

    @BeforeEach
    void setUp() {
        addressService = new AddressService(
                addressPersistencePort, userPersistencePort, addressPolicy, geographyService,
Clock.systemUTC());
    }

    @Test
    void createAddressSetsAsDefaultWhenFirstAddress() {
        when(userPersistencePort.existsById(USER_ID)).thenReturn(true);
        when(addressPolicy.getMaxAddressNumbers()).thenReturn(5L);
        when(geographyService.resolveLocation("VN-HN", "VN-HN-BA", "VN-HN-BA-PX"))
                .thenReturn(resolved());
        when(addressPersistencePort.createAtomically(any(Address.class), eq(5L), eq(true)))
                .thenAnswer(invocation -> {
                    Address address = invocation.getArgument(0);
                    return new Address(
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
                            address.updatedAt());
                });

        Address result = addressService.createAddress(new CreateAddressCommand(
                USER_ID, "Alice", "+84912345678", "VN-HN", "VN-HN-BA", "VN-HN-BA-PX",
                "12 main st", AddressType.HOME, false));

        assertThat(result.isDefault()).isTrue();
        verify(addressPersistencePort).createAtomically(any(Address.class), eq(5L), eq(true));
    }

    @Test
    void createAddressRespectsExplicitDefaultFlag() {
        when(userPersistencePort.existsById(USER_ID)).thenReturn(true);
        when(addressPolicy.getMaxAddressNumbers()).thenReturn(5L);
        when(geographyService.resolveLocation("VN-HN", "VN-HN-BA", "VN-HN-BA-PX"))
                .thenReturn(resolved());
        when(addressPersistencePort.createAtomically(any(Address.class), eq(5L), eq(true)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Address result = addressService.createAddress(new CreateAddressCommand(
                USER_ID, "Alice", "+84912345678", "VN-HN", "VN-HN-BA", "VN-HN-BA-PX",
                "12 main st", AddressType.HOME, true));

        assertThat(result.isDefault()).isTrue();
        verify(addressPersistencePort).createAtomically(any(Address.class), eq(5L), eq(true));
    }

    @Test
    void createAddressLeavesNonDefaultWhenNotFlagged() {
        when(userPersistencePort.existsById(USER_ID)).thenReturn(true);
        when(addressPolicy.getMaxAddressNumbers()).thenReturn(5L);
        when(geographyService.resolveLocation("VN-HN", "VN-HN-BA", "VN-HN-BA-PX"))
                .thenReturn(resolved());
        when(addressPersistencePort.createAtomically(any(Address.class), eq(5L), eq(true)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Address result = addressService.createAddress(new CreateAddressCommand(
                USER_ID, "Alice", "+84912345678", "VN-HN", "VN-HN-BA", "VN-HN-BA-PX",
                "12 main st", AddressType.HOME, false));

        assertThat(!result.isDefault()).isTrue();
        verify(addressPersistencePort).createAtomically(any(Address.class), eq(5L), eq(true));
    }

    @Test
    void createAddressRejectsInvalidPhone() {
        var ex = assertThrows(IdentityException.class,
                () -> addressService.createAddress(new CreateAddressCommand(
                        USER_ID, "Alice", "abc", "VN-HN", "VN-HN-BA", "VN-HN-BA-PX",
                        "12 main st", AddressType.HOME, false)));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.PHONE_INVALID.getCode());
    }

    @Test
    void createAddressRejectsWhenLimitExceeded() {
        when(userPersistencePort.existsById(USER_ID)).thenReturn(true);
        when(addressPolicy.getMaxAddressNumbers()).thenReturn(5L);
        when(geographyService.resolveLocation("VN-HN", "VN-HN-BA", "VN-HN-BA-PX"))
                .thenReturn(resolved());
        when(addressPersistencePort.createAtomically(any(Address.class), eq(5L), eq(true)))
                .thenThrow(new IdentityException(IdentityErrorCode.ADDRESS_NUMBER_EXCEEDED));

        var ex = assertThrows(IdentityException.class,
                () -> addressService.createAddress(new CreateAddressCommand(
                        USER_ID, "Alice", "+84912345678", "VN-HN", "VN-HN-BA", "VN-HN-BA-PX",
                        "12 main st", AddressType.HOME, false)));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.ADDRESS_NUMBER_EXCEEDED.getCode());
    }

    @Test
    void createAddressRejectsMissingUser() {
        when(userPersistencePort.existsById(USER_ID)).thenReturn(false);

        var ex = assertThrows(IdentityException.class,
                () -> addressService.createAddress(new CreateAddressCommand(
                        USER_ID, "Alice", "+84912345678", "VN-HN", "VN-HN-BA", "VN-HN-BA-PX",
                        "12 main st", AddressType.HOME, false)));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.USER_NOT_FOUND.getCode());
    }

    @Test
    void deleteAddressRejectsDefaultAddress() {
        Address defaultAddress = baseAddress(true);
        when(addressPersistencePort.findByAddressIdAndUserId(ADDRESS_ID, USER_ID))
                .thenReturn(Optional.of(defaultAddress));

        var ex = assertThrows(IdentityException.class,
                () -> addressService.deleteAddress(USER_ID, ADDRESS_ID));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.DEFAULT_ADDRESS_CANNOT_BE_DELETED.getCode());
    }

    @Test
    void deleteAddressDeletesNonDefault() {
        Address nonDefault = baseAddress(false);
        when(addressPersistencePort.findByAddressIdAndUserId(ADDRESS_ID, USER_ID))
                .thenReturn(Optional.of(nonDefault));

        addressService.deleteAddress(USER_ID, ADDRESS_ID);

        verify(addressPersistencePort).delete(nonDefault);
    }

    @Test
    void setDefaultAddressClearsOtherDefaults() {
        Address nonDefault = baseAddress(false);
        when(addressPersistencePort.findByAddressIdAndUserId(ADDRESS_ID, USER_ID))
                .thenReturn(Optional.of(nonDefault));
        when(addressPersistencePort.save(any(Address.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Address result = addressService.setDefaultAddress(USER_ID, ADDRESS_ID);

        assertThat(result.isDefault()).isTrue();
        verify(addressPersistencePort).clearDefaultByUserId(USER_ID);
    }

    @Test
    void updateAddressKeepsExistingFullAddressWhenLocationUnchanged() {
        Address existing = baseAddress(false);
        when(addressPersistencePort.findByAddressIdAndUserId(ADDRESS_ID, USER_ID))
                .thenReturn(Optional.of(existing));
        when(addressPersistencePort.save(any(Address.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        addressService.updateAddress(new UpdateAddressCommand(
                USER_ID, ADDRESS_ID, "Bob", "+84912345678",
                existing.provinceCode(), existing.districtCode(), existing.wardCode(),
                existing.detailAddress(), existing.type()));

        verify(addressPersistencePort).save(captor.capture());
        verify(geographyService, never()).resolveLocation(any(), any(), any());
        assertThat(captor.getValue().contactName()).isEqualTo("Bob");
    }

    private Address baseAddress(boolean isDefault) {
        return new Address(
                ADDRESS_ID, USER_ID, "Alice", "+84912345678",
                "VN-HN", "Ha Noi", "VN-HN-BA", "Ba Dinh", "VN-HN-BA-PX", "Phuc Xa",
                "12 main st", "12 main st, Phuc Xa, Ba Dinh, Ha Noi",
                AddressType.HOME, isDefault, Instant.now(), Instant.now());
    }

    private static ResolvedLocation resolved() {
        return new ResolvedLocation(
                new GeographyResult("VN-HN", "Ha Noi", "Hanoi"),
                new GeographyResult("VN-HN-BA", "Ba Dinh", "Ba Dinh"),
                new GeographyResult("VN-HN-BA-PX", "Phuc Xa", "Phuc Xa"));
    }
}
