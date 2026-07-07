package com.aionn.identity.infrastructure.integration;

import com.aionn.identity.application.dto.geography.result.GeographyResult;
import com.aionn.identity.application.dto.geography.result.ResolvedLocation;
import com.aionn.identity.application.service.GeographyService;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.sharedkernel.integration.port.identity.AddressLookupPort.ResolvedAddress;
import com.aionn.sharedkernel.integration.port.identity.AddressLookupPort.ResolvedProvince;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityAddressLookupAdapterTest {

    @Mock
    private GeographyService geographyService;

    private IdentityAddressLookupAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new IdentityAddressLookupAdapter(geographyService);
    }

    @Test
    void resolveReturnsEmptyWhenAnyCodeNull() {
        assertThat(adapter.resolve(null, "d", "w")).isEmpty();
        assertThat(adapter.resolve("p", null, "w")).isEmpty();
        assertThat(adapter.resolve("p", "d", null)).isEmpty();
    }

    @Test
    void resolveMapsLocationToResolvedAddress() {
        ResolvedLocation loc = new ResolvedLocation(
                new GeographyResult("p1", "Province", "ProvinceEn"),
                new GeographyResult("d1", "District", "DistrictEn"),
                new GeographyResult("w1", "Ward", "WardEn"));
        when(geographyService.resolveLocation("p1", "d1", "w1")).thenReturn(loc);

        Optional<ResolvedAddress> result = adapter.resolve("p1", "d1", "w1");

        assertThat(result).isPresent();
        ResolvedAddress addr = result.get();
        assertThat(addr.provinceCode()).isEqualTo("p1");
        assertThat(addr.provinceName()).isEqualTo("Province");
        assertThat(addr.districtCode()).isEqualTo("d1");
        assertThat(addr.districtName()).isEqualTo("District");
        assertThat(addr.wardCode()).isEqualTo("w1");
        assertThat(addr.wardName()).isEqualTo("Ward");
    }

    @Test
    void resolveReturnsEmptyOnInvalidGeographyCode() {
        when(geographyService.resolveLocation("p", "d", "w"))
                .thenThrow(new IdentityException(IdentityErrorCode.INVALID_GEOGRAPHY_CODE));

        assertThat(adapter.resolve("p", "d", "w")).isEmpty();
    }

    @Test
    void resolveRethrowsUnrelatedIdentityException() {
        when(geographyService.resolveLocation("p", "d", "w"))
                .thenThrow(new IdentityException(IdentityErrorCode.USER_NOT_FOUND));

        assertThatThrownBy(() -> adapter.resolve("p", "d", "w"))
                .isInstanceOf(IdentityException.class);
    }

    @Test
    void resolveProvinceReturnsEmptyWhenBlank() {
        assertThat(adapter.resolveProvince(null)).isEmpty();
        assertThat(adapter.resolveProvince("  ")).isEmpty();
    }

    @Test
    void resolveProvinceMapsResult() {
        when(geographyService.getProvince("p1"))
                .thenReturn(new GeographyResult("p1", "Province", "ProvinceEn"));

        Optional<ResolvedProvince> result = adapter.resolveProvince("p1");

        assertThat(result).isPresent();
        assertThat(result.get().code()).isEqualTo("p1");
        assertThat(result.get().name()).isEqualTo("Province");
    }

    @Test
    void resolveProvinceReturnsEmptyOnInvalidGeographyCode() {
        when(geographyService.getProvince("p1"))
                .thenThrow(new IdentityException(IdentityErrorCode.INVALID_GEOGRAPHY_CODE));

        assertThat(adapter.resolveProvince("p1")).isEmpty();
    }

    @Test
    void resolveProvinceRethrowsUnrelatedIdentityException() {
        when(geographyService.getProvince("p1"))
                .thenThrow(new IdentityException(IdentityErrorCode.USER_NOT_FOUND));

        assertThatThrownBy(() -> adapter.resolveProvince("p1"))
                .isInstanceOf(IdentityException.class);
    }
}
