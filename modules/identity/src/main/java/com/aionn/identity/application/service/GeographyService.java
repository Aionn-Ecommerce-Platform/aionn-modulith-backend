package com.aionn.identity.application.service;

import com.aionn.identity.application.dto.geography.result.GeographyResult;
import com.aionn.identity.application.dto.geography.result.ResolvedLocation;
import com.aionn.identity.application.port.out.geography.GeographyPersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GeographyService {

    private final GeographyPersistencePort geographyPersistencePort;

    public List<GeographyResult> listCountries() {
        return geographyPersistencePort.findAllCountries();
    }

    public GeographyResult getCountry(String code) {
        return geographyPersistencePort.findCountryByCode(code)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.INVALID_GEOGRAPHY_CODE,
                        "Country not found: " + code));
    }

    public List<GeographyResult> listProvinces(String countryCode) {
        if (countryCode != null) {
            return geographyPersistencePort.findProvincesByCountryCode(countryCode);
        }
        return geographyPersistencePort.findAllProvinces();
    }

    public GeographyResult getProvince(String code) {
        return geographyPersistencePort.findProvinceByCode(code)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.INVALID_GEOGRAPHY_CODE,
                        "Province not found: " + code));
    }

    public List<GeographyResult> listDistricts(String provinceCode) {
        return geographyPersistencePort.findDistrictsByProvinceCode(provinceCode);
    }

    public GeographyResult getDistrict(String code) {
        return geographyPersistencePort.findDistrictByCode(code)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.INVALID_GEOGRAPHY_CODE,
                        "District not found: " + code));
    }

    public List<GeographyResult> listWards(String districtCode) {
        return geographyPersistencePort.findWardsByDistrictCode(districtCode);
    }

    public GeographyResult getWard(String code) {
        return geographyPersistencePort.findWardByCode(code)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.INVALID_GEOGRAPHY_CODE,
                        "Ward not found: " + code));
    }

    public ResolvedLocation resolveLocation(String provinceCode, String districtCode, String wardCode) {
        return geographyPersistencePort.resolveLocationWithValidation(provinceCode, districtCode, wardCode);
    }
}
