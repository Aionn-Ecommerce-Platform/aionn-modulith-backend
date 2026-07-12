package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.AddressType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AddressTest {

    private Address address(boolean isDefault, AddressType type) {
        Instant now = Instant.now(Clock.systemUTC());
        return new Address(
                "addr-1",
                "user-1",
                "Alice",
                "0912345678",
                "HCM",
                "Ho Chi Minh City",
                "Q1",
                "District 1",
                "W01",
                "Ward 1",
                "123 Nguyen Hue",
                "123 Nguyen Hue, Ward 1, District 1, Ho Chi Minh City",
                type,
                isDefault,
                now,
                now);
    }

    @Test
    void defaultAddressCannotBeDeleted() {
        assertThat(address(true, AddressType.HOME).canBeDeleted()).isFalse();
    }

    @Test
    void nonDefaultAddressCanBeDeleted() {
        assertThat(address(false, AddressType.OFFICE).canBeDeleted()).isTrue();
    }
}
