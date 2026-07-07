package com.aionn.identity.infrastructure.persistence.adapter.geography;

import com.aionn.identity.application.dto.geography.result.GeographyResult;
import com.aionn.identity.application.dto.geography.result.ResolvedLocation;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.infrastructure.persistence.entity.geography.CountryEntity;
import com.aionn.identity.infrastructure.persistence.entity.geography.DistrictEntity;
import com.aionn.identity.infrastructure.persistence.entity.geography.ProvinceEntity;
import com.aionn.identity.infrastructure.persistence.entity.geography.WardEntity;
import com.aionn.identity.infrastructure.persistence.repository.geography.CountryRepository;
import com.aionn.identity.infrastructure.persistence.repository.geography.DistrictRepository;
import com.aionn.identity.infrastructure.persistence.repository.geography.ProvinceRepository;
import com.aionn.identity.infrastructure.persistence.repository.geography.WardRepository;
import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeographyPersistenceAdapterTest {

    @Mock
    private CountryRepository countryRepository;
    @Mock
    private ProvinceRepository provinceRepository;
    @Mock
    private DistrictRepository districtRepository;
    @Mock
    private WardRepository wardRepository;
    @Mock
    private TwoTierCache<String, GeographyResult> cache;

    @InjectMocks
    private GeographyPersistenceAdapter adapter;

    private void cacheLoadsThroughLoader() {
        when(cache.getOrLoad(any(), any())).thenAnswer(invocation -> {
            Supplier<GeographyResult> loader = invocation.getArgument(1);
            return loader.get();
        });
    }

    private CountryEntity country() {
        return new CountryEntity("VN", "Viet Nam", "Vietnam", "+84", true);
    }

    private ProvinceEntity province() {
        return new ProvinceEntity("VN-HN", "Ha Noi", "Hanoi", "VN", true);
    }

    @Test
    void findAllCountriesMapsActiveRows() {
        when(countryRepository.findByActiveTrue()).thenReturn(List.of(country()));

        assertThat(adapter.findAllCountries())
                .containsExactly(new GeographyResult("VN", "Viet Nam", "Vietnam"));
    }

    @Test
    void findCountryByCodeReturnsResultWhenPresent() {
        cacheLoadsThroughLoader();
        when(countryRepository.findByCodeAndActiveTrue("VN")).thenReturn(Optional.of(country()));

        assertThat(adapter.findCountryByCode("VN"))
                .contains(new GeographyResult("VN", "Viet Nam", "Vietnam"));
    }

    @Test
    void findCountryByCodeReturnsEmptyWhenMissing() {
        cacheLoadsThroughLoader();
        when(countryRepository.findByCodeAndActiveTrue("XX")).thenReturn(Optional.empty());

        assertThat(adapter.findCountryByCode("XX")).isEmpty();
    }

    @Test
    void findCountryByCodeReturnsEmptyWhenCacheReturnsSentinel() {
        when(cache.getOrLoad(eq("country:VN"), any())).thenReturn(new GeographyResult(null, null, null));

        assertThat(adapter.findCountryByCode("VN")).isEmpty();
    }

    @Test
    void findProvincesByCountryCodeMapsRows() {
        when(provinceRepository.findByCountryCodeAndActiveTrue("VN")).thenReturn(List.of(province()));

        assertThat(adapter.findProvincesByCountryCode("VN"))
                .containsExactly(new GeographyResult("VN-HN", "Ha Noi", "Hanoi"));
    }

    @Test
    void findAllProvincesMapsRows() {
        when(provinceRepository.findByActiveTrue()).thenReturn(List.of(province()));

        assertThat(adapter.findAllProvinces())
                .containsExactly(new GeographyResult("VN-HN", "Ha Noi", "Hanoi"));
    }

    @Test
    void findProvinceByCodeReturnsResultWhenPresent() {
        cacheLoadsThroughLoader();
        when(provinceRepository.findByCodeAndActiveTrue("VN-HN")).thenReturn(Optional.of(province()));

        assertThat(adapter.findProvinceByCode("VN-HN"))
                .contains(new GeographyResult("VN-HN", "Ha Noi", "Hanoi"));
    }

    @Test
    void findProvinceByCodeReturnsEmptyWhenMissing() {
        cacheLoadsThroughLoader();
        when(provinceRepository.findByCodeAndActiveTrue("VN-HN")).thenReturn(Optional.empty());

        assertThat(adapter.findProvinceByCode("VN-HN")).isEmpty();
    }

    @Test
    void findDistrictsByProvinceCodeMapsRows() {
        DistrictEntity district = mock(DistrictEntity.class);
        when(district.getCode()).thenReturn("VN-HN-BA");
        when(district.getName()).thenReturn("Ba Dinh");
        when(district.getNameEn()).thenReturn("Ba Dinh");
        when(districtRepository.findByProvinceCodeAndActiveTrue("VN-HN")).thenReturn(List.of(district));

        assertThat(adapter.findDistrictsByProvinceCode("VN-HN"))
                .containsExactly(new GeographyResult("VN-HN-BA", "Ba Dinh", "Ba Dinh"));
    }

    @Test
    void findDistrictByCodeReturnsResultWhenPresent() {
        cacheLoadsThroughLoader();
        DistrictEntity district = mock(DistrictEntity.class);
        when(district.getCode()).thenReturn("VN-HN-BA");
        when(district.getName()).thenReturn("Ba Dinh");
        when(district.getNameEn()).thenReturn("Ba Dinh");
        when(districtRepository.findByCodeAndActiveTrue("VN-HN-BA")).thenReturn(Optional.of(district));

        assertThat(adapter.findDistrictByCode("VN-HN-BA"))
                .contains(new GeographyResult("VN-HN-BA", "Ba Dinh", "Ba Dinh"));
    }

    @Test
    void findDistrictByCodeReturnsEmptyWhenMissing() {
        cacheLoadsThroughLoader();
        when(districtRepository.findByCodeAndActiveTrue("VN-HN-BA")).thenReturn(Optional.empty());

        assertThat(adapter.findDistrictByCode("VN-HN-BA")).isEmpty();
    }

    @Test
    void findWardsByDistrictCodeMapsRows() {
        WardEntity ward = mock(WardEntity.class);
        when(ward.getCode()).thenReturn("VN-HN-BA-PX");
        when(ward.getName()).thenReturn("Phuc Xa");
        when(ward.getNameEn()).thenReturn("Phuc Xa");
        when(wardRepository.findByDistrictCodeAndActiveTrue("VN-HN-BA")).thenReturn(List.of(ward));

        assertThat(adapter.findWardsByDistrictCode("VN-HN-BA"))
                .containsExactly(new GeographyResult("VN-HN-BA-PX", "Phuc Xa", "Phuc Xa"));
    }

    @Test
    void findWardByCodeReturnsResultWhenPresent() {
        cacheLoadsThroughLoader();
        WardEntity ward = mock(WardEntity.class);
        when(ward.getCode()).thenReturn("VN-HN-BA-PX");
        when(ward.getName()).thenReturn("Phuc Xa");
        when(ward.getNameEn()).thenReturn("Phuc Xa");
        when(wardRepository.findByCodeAndActiveTrue("VN-HN-BA-PX")).thenReturn(Optional.of(ward));

        assertThat(adapter.findWardByCode("VN-HN-BA-PX"))
                .contains(new GeographyResult("VN-HN-BA-PX", "Phuc Xa", "Phuc Xa"));
    }

    @Test
    void findWardByCodeReturnsEmptyWhenMissing() {
        cacheLoadsThroughLoader();
        when(wardRepository.findByCodeAndActiveTrue("VN-HN-BA-PX")).thenReturn(Optional.empty());

        assertThat(adapter.findWardByCode("VN-HN-BA-PX")).isEmpty();
    }

    @Test
    void resolveLocationWithValidationReturnsResolvedLocation() {
        WardEntity ward = mockWard("VN-HN-BA-PX", "VN-HN-BA", "VN-HN");

        ResolvedLocation resolved = adapter.resolveLocationWithValidation("VN-HN", "VN-HN-BA", "VN-HN-BA-PX");

        assertThat(resolved.province().code()).isEqualTo("VN-HN");
        assertThat(resolved.district().code()).isEqualTo("VN-HN-BA");
        assertThat(resolved.ward().code()).isEqualTo("VN-HN-BA-PX");
    }

    @Test
    void resolveLocationWithValidationThrowsWhenWardMissing() {
        when(wardRepository.findByCodeWithDistrictAndProvince("VN-HN-BA-PX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.resolveLocationWithValidation("VN-HN", "VN-HN-BA", "VN-HN-BA-PX"))
                .isInstanceOfSatisfying(IdentityException.class,
                        ex -> assertThat(ex.getErrorCode())
                                .isEqualTo(IdentityErrorCode.INVALID_GEOGRAPHY_CODE.getCode()));
    }

    @Test
    void resolveLocationWithValidationThrowsWhenWardNotInDistrict() {
        mockWard("VN-HN-BA-PX", "VN-HN-BA", "VN-HN");

        assertThatThrownBy(() -> adapter.resolveLocationWithValidation("VN-HN", "OTHER", "VN-HN-BA-PX"))
                .isInstanceOfSatisfying(IdentityException.class,
                        ex -> assertThat(ex.getErrorCode())
                                .isEqualTo(IdentityErrorCode.INVALID_GEOGRAPHY_CODE.getCode()));
    }

    @Test
    void resolveLocationWithValidationThrowsWhenDistrictNotInProvince() {
        mockWard("VN-HN-BA-PX", "VN-HN-BA", "VN-HN");

        assertThatThrownBy(() -> adapter.resolveLocationWithValidation("OTHER", "VN-HN-BA", "VN-HN-BA-PX"))
                .isInstanceOfSatisfying(IdentityException.class,
                        ex -> assertThat(ex.getErrorCode())
                                .isEqualTo(IdentityErrorCode.INVALID_GEOGRAPHY_CODE.getCode()));
    }

    private WardEntity mockWard(String wardCode, String districtCode, String provinceCode) {
        ProvinceEntity province = mock(ProvinceEntity.class);
        lenient().when(province.getCode()).thenReturn(provinceCode);
        lenient().when(province.getName()).thenReturn("Ha Noi");
        lenient().when(province.getNameEn()).thenReturn("Hanoi");
        DistrictEntity district = mock(DistrictEntity.class);
        lenient().when(district.getProvince()).thenReturn(province);
        lenient().when(district.getProvinceCode()).thenReturn(provinceCode);
        lenient().when(district.getCode()).thenReturn(districtCode);
        lenient().when(district.getName()).thenReturn("Ba Dinh");
        lenient().when(district.getNameEn()).thenReturn("Ba Dinh");
        WardEntity ward = mock(WardEntity.class);
        lenient().when(ward.getDistrict()).thenReturn(district);
        lenient().when(ward.getDistrictCode()).thenReturn(districtCode);
        lenient().when(ward.getCode()).thenReturn(wardCode);
        lenient().when(ward.getName()).thenReturn("Phuc Xa");
        lenient().when(ward.getNameEn()).thenReturn("Phuc Xa");
        lenient().when(wardRepository.findByCodeWithDistrictAndProvince(wardCode)).thenReturn(Optional.of(ward));
        return ward;
    }
}
