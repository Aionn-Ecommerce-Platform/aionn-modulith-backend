package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.AddressType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class AddressTest {

    @Test
    void constructor_validInput_createsInstance() {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());

        Address address = new Address(
                "addr-123",
                "user-456",
                "Alice Smith",
                "0912345678",
                "HCM",
                "Ho Chi Minh City",
                "Q1",
                "District 1",
                "W01",
                "Ward 1",
                "123 Nguyen Hue",
                "123 Nguyen Hue, Ward 1, District 1, Ho Chi Minh City",
                AddressType.HOME,
                true,
                now,
                now);

        assertThat(address.addressId()).isEqualTo("addr-123");
        assertThat(address.userId()).isEqualTo("user-456");
        assertThat(address.contactName()).isEqualTo("Alice Smith");
        assertThat(address.phone()).isEqualTo("0912345678");
        assertThat(address.provinceCode()).isEqualTo("HCM");
        assertThat(address.provinceName()).isEqualTo("Ho Chi Minh City");
        assertThat(address.districtCode()).isEqualTo("Q1");
        assertThat(address.districtName()).isEqualTo("District 1");
        assertThat(address.wardCode()).isEqualTo("W01");
        assertThat(address.wardName()).isEqualTo("Ward 1");
        assertThat(address.detailAddress()).isEqualTo("123 Nguyen Hue");
        assertThat(address.fullAddress()).isEqualTo("123 Nguyen Hue, Ward 1, District 1, Ho Chi Minh City");
        assertThat(address.type()).isEqualTo(AddressType.HOME);
        assertThat(address.isDefault()).isTrue();
        assertThat(address.createdAt()).isEqualTo(now);
        assertThat(address.updatedAt()).isEqualTo(now);
    }

    @Test
    void canBeDeleted_defaultAddress_returnsFalse() {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        Address address = new Address(
                "addr-default",
                "user-123",
                "John Doe",
                "0987654321",
                "HN",
                "Hanoi",
                "HBT",
                "Hoan Kiem",
                "W01",
                "Ward 1",
                "456 Tran Hung Dao",
                "456 Tran Hung Dao, Ward 1, Hoan Kiem, Hanoi",
                AddressType.HOME,
                true,
                now,
                now);

        assertThat(address.canBeDeleted()).isFalse();
    }

    @Test
    void canBeDeleted_nonDefaultAddress_returnsTrue() {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        Address address = new Address(
                "addr-secondary",
                "user-123",
                "John Doe",
                "0987654321",
                "DN",
                "Da Nang",
                "HC",
                "Hai Chau",
                "W02",
                "Ward 2",
                "789 Bach Dang",
                "789 Bach Dang, Ward 2, Hai Chau, Da Nang",
                AddressType.OFFICE,
                false,
                now,
                now);

        assertThat(address.canBeDeleted()).isTrue();
    }

    @Test
    void recordsAreEqualWhenAllFieldsMatch() {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        Address address1 = new Address(
                "addr-123",
                "user-456",
                "Alice",
                "0912345678",
                "HCM",
                "Ho Chi Minh City",
                "Q1",
                "District 1",
                "W01",
                "Ward 1",
                "123 Street",
                "123 Street, Ward 1, District 1, Ho Chi Minh City",
                AddressType.HOME,
                true,
                now,
                now);

        Address address2 = new Address(
                "addr-123",
                "user-456",
                "Alice",
                "0912345678",
                "HCM",
                "Ho Chi Minh City",
                "Q1",
                "District 1",
                "W01",
                "Ward 1",
                "123 Street",
                "123 Street, Ward 1, District 1, Ho Chi Minh City",
                AddressType.HOME,
                true,
                now,
                now);

        assertThat(address1).isEqualTo(address2);
    }

    @Test
    void recordsAreNotEqualWhenAddressIdDiffers() {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        Address address1 = new Address(
                "addr-123",
                "user-456",
                "Alice",
                "0912345678",
                "HCM",
                "Ho Chi Minh City",
                "Q1",
                "District 1",
                "W01",
                "Ward 1",
                "123 Street",
                "Full Address",
                AddressType.HOME,
                true,
                now,
                now);

        Address address2 = new Address(
                "addr-456",
                "user-456",
                "Alice",
                "0912345678",
                "HCM",
                "Ho Chi Minh City",
                "Q1",
                "District 1",
                "W01",
                "Ward 1",
                "123 Street",
                "Full Address",
                AddressType.HOME,
                true,
                now,
                now);

        assertThat(address1).isNotEqualTo(address2);
    }
}
