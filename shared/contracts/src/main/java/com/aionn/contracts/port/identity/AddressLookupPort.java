package com.aionn.contracts.port.identity;

import java.util.Optional;

public interface AddressLookupPort {

    Optional<ResolvedAddress> resolve(String provinceCode, String districtCode, String wardCode);

    /**
     * Resolves free-form province/locality names (or codes) to canonical codes. Used by protocol
     * adapters whose address model does not carry a separate district field.
     */
    default Optional<ResolvedAddress> resolveByNames(String countryCode, String province, String locality) {
        return Optional.empty();
    }

    Optional<ResolvedProvince> resolveProvince(String provinceCode);

    record ResolvedAddress(
            String provinceCode,
            String provinceName,
            String districtCode,
            String districtName,
            String wardCode,
            String wardName) {
    }

    record ResolvedProvince(String code, String name) {
    }
}
