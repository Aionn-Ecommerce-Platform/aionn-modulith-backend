package com.aionn.identity.infrastructure.persistence.adapter.geography;

import com.aionn.identity.application.dto.geography.result.GeographyResult;
import com.aionn.identity.application.dto.geography.result.ResolvedLocation;
import com.aionn.identity.application.port.out.geography.GeographyPersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.infrastructure.persistence.entity.geography.DistrictEntity;
import com.aionn.identity.infrastructure.persistence.entity.geography.ProvinceEntity;
import com.aionn.identity.infrastructure.persistence.entity.geography.WardEntity;
import com.aionn.identity.infrastructure.persistence.repository.geography.CountryRepository;
import com.aionn.identity.infrastructure.persistence.repository.geography.DistrictRepository;
import com.aionn.identity.infrastructure.persistence.repository.geography.ProvinceRepository;
import com.aionn.identity.infrastructure.persistence.repository.geography.WardRepository;
import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCache;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Component
public class GeographyPersistenceAdapter implements GeographyPersistencePort {

    // Sentinel written to the cache when a code is confirmed absent. Distinct
    // instance (code=null) so a real GeographyResult can never collide with it.
    private static final GeographyResult MISS_SENTINEL = new GeographyResult(null, null, null);

    private final CountryRepository countryRepository;
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;
    private final TwoTierCache<String, GeographyResult> cache;

    public GeographyPersistenceAdapter(
            CountryRepository countryRepository,
            ProvinceRepository provinceRepository,
            DistrictRepository districtRepository,
            WardRepository wardRepository,
            @Qualifier("identityGeographyCache") TwoTierCache<String, GeographyResult> cache) {
        this.countryRepository = countryRepository;
        this.provinceRepository = provinceRepository;
        this.districtRepository = districtRepository;
        this.wardRepository = wardRepository;
        this.cache = cache;
    }

    @Override
    public List<GeographyResult> findAllCountries() {
        return countryRepository.findByActiveTrue().stream()
                .map(c -> new GeographyResult(c.getCode(), c.getName(), c.getNameEn()))
                .toList();
    }

    @Override
    public Optional<GeographyResult> findCountryByCode(String code) {
        return lookupCached("country:" + code, () -> countryRepository
                .findByCodeAndActiveTrue(code)
                .map(c -> new GeographyResult(c.getCode(), c.getName(), c.getNameEn())));
    }

    @Override
    public List<GeographyResult> findProvincesByCountryCode(String countryCode) {
        return provinceRepository.findByCountryCodeAndActiveTrue(countryCode).stream()
                .map(p -> new GeographyResult(p.getCode(), p.getName(), p.getNameEn()))
                .toList();
    }

    @Override
    public List<GeographyResult> findAllProvinces() {
        return provinceRepository.findByActiveTrue().stream()
                .map(p -> new GeographyResult(p.getCode(), p.getName(), p.getNameEn()))
                .toList();
    }

    @Override
    public Optional<GeographyResult> findProvinceByCode(String code) {
        return lookupCached("province:" + code, () -> provinceRepository
                .findByCodeAndActiveTrue(code)
                .map(p -> new GeographyResult(p.getCode(), p.getName(), p.getNameEn())));
    }

    @Override
    public List<GeographyResult> findDistrictsByProvinceCode(String provinceCode) {
        return districtRepository.findByProvinceCodeAndActiveTrue(provinceCode).stream()
                .map(d -> new GeographyResult(d.getCode(), d.getName(), d.getNameEn()))
                .toList();
    }

    @Override
    public Optional<GeographyResult> findDistrictByCode(String code) {
        return lookupCached("district:" + code, () -> districtRepository
                .findByCodeAndActiveTrue(code)
                .map(d -> new GeographyResult(d.getCode(), d.getName(), d.getNameEn())));
    }

    @Override
    public List<GeographyResult> findWardsByDistrictCode(String districtCode) {
        return wardRepository.findByDistrictCodeAndActiveTrue(districtCode).stream()
                .map(w -> new GeographyResult(w.getCode(), w.getName(), w.getNameEn()))
                .toList();
    }

    @Override
    public Optional<GeographyResult> findWardByCode(String code) {
        return lookupCached("ward:" + code, () -> wardRepository
                .findByCodeAndActiveTrue(code)
                .map(w -> new GeographyResult(w.getCode(), w.getName(), w.getNameEn())));
    }

    // Cache both hits and misses. Misses are cached as MISS_SENTINEL to spare
    // subsequent lookups a DB round-trip when unknown codes keep coming in.
    private Optional<GeographyResult> lookupCached(String cacheKey, Supplier<Optional<GeographyResult>> loader) {
        GeographyResult cached = cache.getOrLoad(cacheKey, () -> loader.get().orElse(MISS_SENTINEL));
        if (cached == null || cached.code() == null) {
            return Optional.empty();
        }
        return Optional.of(cached);
    }

    @Override
    public ResolvedLocation resolveLocationWithValidation(String provinceCode, String districtCode, String wardCode) {
        WardEntity ward = wardRepository.findByCodeWithDistrictAndProvince(wardCode)
                .orElseThrow(() -> new IdentityException(
                        IdentityErrorCode.INVALID_GEOGRAPHY_CODE,
                        "Invalid ward code: " + wardCode));

        DistrictEntity district = ward.getDistrict();
        ProvinceEntity province = district.getProvince();

        if (!ward.getDistrictCode().equals(districtCode)) {
            throw new IdentityException(
                    IdentityErrorCode.INVALID_GEOGRAPHY_CODE,
                    "Ward " + wardCode + " does not belong to district " + districtCode);
        }

        if (!district.getProvinceCode().equals(provinceCode)) {
            throw new IdentityException(
                    IdentityErrorCode.INVALID_GEOGRAPHY_CODE,
                    "District " + districtCode + " does not belong to province " + provinceCode);
        }

        return new ResolvedLocation(
                new GeographyResult(province.getCode(), province.getName(), province.getNameEn()),
                new GeographyResult(district.getCode(), district.getName(), district.getNameEn()),
                new GeographyResult(ward.getCode(), ward.getName(), ward.getNameEn()));
    }
}
