package com.aionn.identity.infrastructure.integration;

import com.aionn.identity.application.dto.geography.result.GeographyResult;
import com.aionn.identity.application.dto.geography.result.ResolvedLocation;
import com.aionn.identity.application.service.GeographyService;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.sharedkernel.integration.port.identity.AddressLookupPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityAddressLookupAdapter implements AddressLookupPort {

    private final GeographyService geographyService;

    @Override
    public Optional<ResolvedAddress> resolve(String provinceCode, String districtCode, String wardCode) {
        if (provinceCode == null || districtCode == null || wardCode == null) {
            return Optional.empty();
        }
        try {
            ResolvedLocation loc = geographyService.resolveLocation(provinceCode, districtCode, wardCode);
            return Optional.of(new ResolvedAddress(
                    loc.province().code(), loc.province().name(),
                    loc.district().code(), loc.district().name(),
                    loc.ward().code(), loc.ward().name()));
        } catch (IdentityException ex) {
            if (IdentityErrorCode.INVALID_GEOGRAPHY_CODE.getCode().equals(ex.getErrorCode())) {
                log.debug("Address lookup miss province={} district={} ward={}",
                        provinceCode, districtCode, wardCode);
                return Optional.empty();
            }
            throw ex;
        }
    }

    @Override
    public Optional<ResolvedProvince> resolveProvince(String provinceCode) {
        if (provinceCode == null || provinceCode.isBlank()) {
            return Optional.empty();
        }
        try {
            GeographyResult province = geographyService.getProvince(provinceCode);
            return Optional.of(new ResolvedProvince(province.code(), province.name()));
        } catch (IdentityException ex) {
            if (IdentityErrorCode.INVALID_GEOGRAPHY_CODE.getCode().equals(ex.getErrorCode())) {
                log.debug("Province lookup miss code={}", provinceCode);
                return Optional.empty();
            }
            throw ex;
        }
    }
}
