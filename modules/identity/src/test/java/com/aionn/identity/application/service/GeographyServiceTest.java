package com.aionn.identity.application.service;

import com.aionn.identity.application.dto.geography.result.GeographyResult;
import com.aionn.identity.application.dto.geography.result.ResolvedLocation;
import com.aionn.identity.application.port.out.geography.GeographyPersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeographyServiceTest {

    @Mock
    private GeographyPersistencePort geographyPersistencePort;

    private GeographyService geographyService;

    @BeforeEach
    void setUp() {
        geographyService = new GeographyService(geographyPersistencePort);
    }

    @Test
    void listCountriesReturnsAll() {
        var vn = new GeographyResult("VN", "Viet Nam", "Vietnam");
        when(geographyPersistencePort.findAllCountries()).thenReturn(List.of(vn));

        List<GeographyResult> result = geographyService.listCountries();

        assertEquals(List.of(vn), result);
    }

    @Test
    void getCountryThrowsWhenNotFound() {
        when(geographyPersistencePort.findCountryByCode("XX")).thenReturn(Optional.empty());

        var ex = assertThrows(IdentityException.class, () -> geographyService.getCountry("XX"));

        assertEquals(IdentityErrorCode.INVALID_GEOGRAPHY_CODE.getCode(), ex.getErrorCode());
    }

    @Test
    void listProvincesScopesByCountryWhenProvided() {
        var hn = new GeographyResult("VN-HN", "Ha Noi", "Hanoi");
        when(geographyPersistencePort.findProvincesByCountryCode("VN")).thenReturn(List.of(hn));

        List<GeographyResult> result = geographyService.listProvinces("VN");

        assertEquals(List.of(hn), result);
        verify(geographyPersistencePort).findProvincesByCountryCode("VN");
    }

    @Test
    void listProvincesFallsBackToAllWhenCountryNull() {
        when(geographyPersistencePort.findAllProvinces()).thenReturn(List.of());

        geographyService.listProvinces(null);

        verify(geographyPersistencePort).findAllProvinces();
    }

    @Test
    void resolveLocationDelegatesToPort() {
        var province = new GeographyResult("VN-HN", "Ha Noi", "Hanoi");
        var district = new GeographyResult("VN-HN-BA", "Ba Dinh", "Ba Dinh");
        var ward = new GeographyResult("VN-HN-BA-PX", "Phuc Xa", "Phuc Xa");
        var resolved = new ResolvedLocation(province, district, ward);
        when(geographyPersistencePort.resolveLocationWithValidation("VN-HN", "VN-HN-BA", "VN-HN-BA-PX"))
                .thenReturn(resolved);

        ResolvedLocation result = geographyService.resolveLocation("VN-HN", "VN-HN-BA", "VN-HN-BA-PX");

        assertEquals(resolved, result);
    }

    @Test
    void getProvinceThrowsWhenNotFound() {
        when(geographyPersistencePort.findProvinceByCode("VN-XX")).thenReturn(Optional.empty());

        var ex = assertThrows(IdentityException.class, () -> geographyService.getProvince("VN-XX"));

        assertEquals(IdentityErrorCode.INVALID_GEOGRAPHY_CODE.getCode(), ex.getErrorCode());
    }

    @Test
    void listDistrictsAndWardsDelegate() {
        when(geographyPersistencePort.findDistrictsByProvinceCode("VN-HN")).thenReturn(List.of());
        when(geographyPersistencePort.findWardsByDistrictCode("VN-HN-BA")).thenReturn(List.of());

        geographyService.listDistricts("VN-HN");
        geographyService.listWards("VN-HN-BA");

        verify(geographyPersistencePort).findDistrictsByProvinceCode("VN-HN");
        verify(geographyPersistencePort).findWardsByDistrictCode("VN-HN-BA");
    }
}
