package com.aionn.sharedkernel.integration.port.identity;

import java.util.Optional;

public interface AddressLookupPort {

    Optional<ResolvedAddress> resolve(String provinceCode, String districtCode, String wardCode);

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
