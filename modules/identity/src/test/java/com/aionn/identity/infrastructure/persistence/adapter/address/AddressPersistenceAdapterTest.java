package com.aionn.identity.infrastructure.persistence.adapter.address;

import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.Address;
import com.aionn.identity.domain.valueobject.AddressType;
import com.aionn.identity.infrastructure.persistence.entity.UserAddressEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.mapper.AddressDomainMapper;
import com.aionn.identity.infrastructure.persistence.repository.address.AddressRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressPersistenceAdapterTest {

    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String ADDRESS_ID = "01HZADDR000000000000000000";

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AddressDomainMapper addressDomainMapper;

    @InjectMocks
    private AddressPersistenceAdapter adapter;

    private Address address(boolean isDefault) {
        return new Address(
                ADDRESS_ID, USER_ID, "Alice", "+84912345678",
                "VN-HN", "Ha Noi", "VN-HN-BA", "Ba Dinh", "VN-HN-BA-PX", "Phuc Xa",
                "12 main st", "12 main st, Phuc Xa, Ba Dinh, Ha Noi",
                AddressType.HOME, isDefault, Instant.now(), Instant.now());
    }

    @Test
    void findByUserIdMapsEntities() {
        UserAddressEntity entity = mock(UserAddressEntity.class);
        Address domain = address(false);
        when(addressRepository.findByUser_UserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(entity));
        when(addressDomainMapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findByUserId(USER_ID)).containsExactly(domain);
    }

    @Test
    void countByUserIdDelegates() {
        when(addressRepository.countByUser_UserId(USER_ID)).thenReturn(3L);

        assertThat(adapter.countByUserId(USER_ID)).isEqualTo(3L);
    }

    @Test
    void createAtomicallyThrowsWhenUserMissing() {
        Address address = address(false);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.createAtomically(address, 5, true))
                .isInstanceOfSatisfying(IdentityException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.USER_NOT_FOUND.getCode()));
    }

    @Test
    void createAtomicallyThrowsWhenLimitReached() {
        Address address = address(false);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(UserEntity.builder().build()));
        when(addressRepository.countByUser_UserId(USER_ID)).thenReturn(5L);

        assertThatThrownBy(() -> adapter.createAtomically(address, 5, true))
                .isInstanceOfSatisfying(IdentityException.class,
                        ex -> assertThat(ex.getErrorCode())
                                .isEqualTo(IdentityErrorCode.ADDRESS_NUMBER_EXCEEDED.getCode()));
    }

    @Test
    void createAtomicallyPromotesFirstAddressToDefault() {
        Address address = address(false);
        UserAddressEntity entity = mock(UserAddressEntity.class);
        Address saved = address(true);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(UserEntity.builder().build()));
        when(addressRepository.countByUser_UserId(USER_ID)).thenReturn(0L);
        when(addressDomainMapper.toEntity(any(Address.class), any(UserEntity.class))).thenReturn(entity);
        when(addressRepository.save(entity)).thenReturn(entity);
        when(addressDomainMapper.toDomain(entity)).thenReturn(saved);

        Address result = adapter.createAtomically(address, 5, true);

        assertThat(result).isSameAs(saved);
        verify(addressRepository).clearDefaultAddressByUserId(USER_ID);
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressDomainMapper).toEntity(captor.capture(), any(UserEntity.class));
        assertThat(captor.getValue().isDefault()).isTrue();
    }

    @Test
    void createAtomicallyKeepsExplicitDefaultAndClearsOthers() {
        Address address = address(true);
        UserAddressEntity entity = mock(UserAddressEntity.class);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(UserEntity.builder().build()));
        when(addressRepository.countByUser_UserId(USER_ID)).thenReturn(2L);
        when(addressDomainMapper.toEntity(any(Address.class), any(UserEntity.class))).thenReturn(entity);
        when(addressRepository.save(entity)).thenReturn(entity);
        when(addressDomainMapper.toDomain(entity)).thenReturn(address);

        adapter.createAtomically(address, 5, false);

        verify(addressRepository).clearDefaultAddressByUserId(USER_ID);
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressDomainMapper).toEntity(captor.capture(), any(UserEntity.class));
        assertThat(captor.getValue()).isSameAs(address);
    }

    @Test
    void createAtomicallyLeavesNonDefaultWhenNotFirstAndNotFlagged() {
        Address address = address(false);
        UserAddressEntity entity = mock(UserAddressEntity.class);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(UserEntity.builder().build()));
        when(addressRepository.countByUser_UserId(USER_ID)).thenReturn(1L);
        when(addressDomainMapper.toEntity(any(Address.class), any(UserEntity.class))).thenReturn(entity);
        when(addressRepository.save(entity)).thenReturn(entity);
        when(addressDomainMapper.toDomain(entity)).thenReturn(address);

        adapter.createAtomically(address, 5, false);

        verify(addressRepository, never()).clearDefaultAddressByUserId(any());
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressDomainMapper).toEntity(captor.capture(), any(UserEntity.class));
        assertThat(captor.getValue().isDefault()).isFalse();
    }

    @Test
    void saveMapsThroughEntityAndBack() {
        Address address = address(false);
        UserAddressEntity entity = mock(UserAddressEntity.class);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(UserEntity.builder().build());
        when(addressDomainMapper.toEntity(eq(address), any(UserEntity.class))).thenReturn(entity);
        when(addressRepository.save(entity)).thenReturn(entity);
        when(addressDomainMapper.toDomain(entity)).thenReturn(address);

        assertThat(adapter.save(address)).isSameAs(address);
    }

    @Test
    void findByAddressIdAndUserIdReturnsMappedWhenPresent() {
        UserAddressEntity entity = mock(UserAddressEntity.class);
        Address domain = address(false);
        when(addressRepository.findByAddressIdAndUser_UserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(entity));
        when(addressDomainMapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findByAddressIdAndUserId(ADDRESS_ID, USER_ID)).contains(domain);
    }

    @Test
    void findByAddressIdAndUserIdReturnsEmptyWhenMissing() {
        when(addressRepository.findByAddressIdAndUser_UserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.empty());

        assertThat(adapter.findByAddressIdAndUserId(ADDRESS_ID, USER_ID)).isEmpty();
    }

    @Test
    void clearDefaultByUserIdDelegates() {
        adapter.clearDefaultByUserId(USER_ID);

        verify(addressRepository).clearDefaultAddressByUserId(USER_ID);
    }

    @Test
    void deleteRemovesByAddressId() {
        adapter.delete(address(false));

        verify(addressRepository).deleteById(ADDRESS_ID);
    }
}
