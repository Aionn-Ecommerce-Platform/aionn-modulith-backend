package com.aionn.identity.infrastructure.integration.address;

import com.aionn.identity.application.dto.geography.result.GeographyResult;
import com.aionn.identity.application.dto.geography.result.ResolvedLocation;
import com.aionn.identity.application.service.GeographyService;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.contracts.port.identity.AddressLookupPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    public Optional<ResolvedAddress> resolveByNames(String countryCode, String province, String locality) {
        if (province == null || province.isBlank() || locality == null || locality.isBlank()) {
            return Optional.empty();
        }
        List<GeographyResult> provinces = countryCode == null || countryCode.isBlank()
                ? geographyService.listProvinces(null)
                : geographyService.listProvinces(countryCode);
        GeographyResult matchedProvince = matchOne(provinces, province);
        if (matchedProvince == null) {
            return Optional.empty();
        }

        List<ResolvedAddress> matches = new ArrayList<>();
        for (GeographyResult district : geographyService.listDistricts(matchedProvince.code())) {
            for (GeographyResult ward : geographyService.listWards(district.code())) {
                if (matches(ward, locality)) {
                    matches.add(new ResolvedAddress(
                            matchedProvince.code(), matchedProvince.name(),
                            district.code(), district.name(), ward.code(), ward.name()));
                }
            }
        }
        // Ambiguous locality names (for example duplicated ward names in one province)
        // are rejected rather than silently routing a parcel to the wrong ward.
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }

    private static GeographyResult matchOne(List<GeographyResult> values, String target) {
        for (GeographyResult value : values) {
            if (target.equalsIgnoreCase(value.code())) {
                return value;
            }
        }
        GeographyResult nameMatch = null;
        for (GeographyResult value : values) {
            if (matches(value, target)) {
                if (nameMatch != null) {
                    return null;
                }
                nameMatch = value;
            }
        }
        return nameMatch;
    }

    private static boolean matches(GeographyResult value, String target) {
        if (target.equalsIgnoreCase(value.code())) {
            return true;
        }
        String normalizedTarget = normalize(target);
        return matchesName(value.name(), normalizedTarget) || matchesName(value.nameEn(), normalizedTarget);
    }

    private static boolean matchesName(String name, String normalizedTarget) {
        if (normalizedTarget.isEmpty()) {
            return false;
        }
        String normalizedName = normalize(name);
        return normalizedName.equals(normalizedTarget)
                || normalizedName.endsWith(normalizedTarget)
                || normalizedTarget.endsWith(normalizedName);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
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
