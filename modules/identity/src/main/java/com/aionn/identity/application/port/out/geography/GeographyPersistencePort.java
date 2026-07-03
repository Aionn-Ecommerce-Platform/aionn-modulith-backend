package com.aionn.identity.application.port.out.geography;

import com.aionn.identity.application.dto.geography.result.GeographyResult;
import com.aionn.identity.application.dto.geography.result.ResolvedLocation;

import java.util.List;
import java.util.Optional;

public interface GeographyPersistencePort {
    List<GeographyResult> findAllCountries();

    Optional<GeographyResult> findCountryByCode(String code);

    List<GeographyResult> findProvincesByCountryCode(String countryCode);

    List<GeographyResult> findAllProvinces();

    Optional<GeographyResult> findProvinceByCode(String code);

    List<GeographyResult> findDistrictsByProvinceCode(String provinceCode);

    Optional<GeographyResult> findDistrictByCode(String code);

    List<GeographyResult> findWardsByDistrictCode(String districtCode);

    Optional<GeographyResult> findWardByCode(String code);

    ResolvedLocation resolveLocationWithValidation(String provinceCode, String districtCode, String wardCode);
}
