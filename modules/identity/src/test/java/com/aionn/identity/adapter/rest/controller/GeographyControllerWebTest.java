package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.geography.response.GeographyResponse;
import com.aionn.identity.adapter.rest.exception.IdentityExceptionHandler;
import com.aionn.identity.adapter.rest.mapper.geography.GeographyDtoMapper;
import com.aionn.identity.application.dto.geography.result.GeographyResult;
import com.aionn.identity.application.port.in.geography.GetCountryQueryPort;
import com.aionn.identity.application.port.in.geography.GetDistrictQueryPort;
import com.aionn.identity.application.port.in.geography.GetProvinceQueryPort;
import com.aionn.identity.application.port.in.geography.GetWardQueryPort;
import com.aionn.identity.application.port.in.geography.ListCountriesQueryPort;
import com.aionn.identity.application.port.in.geography.ListDistrictsQueryPort;
import com.aionn.identity.application.port.in.geography.ListProvincesQueryPort;
import com.aionn.identity.application.port.in.geography.ListWardsQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GeographyControllerWebTest {

    @Mock
    private ListCountriesQueryPort listCountriesQueryPort;
    @Mock
    private GetCountryQueryPort getCountryQueryPort;
    @Mock
    private ListProvincesQueryPort listProvincesQueryPort;
    @Mock
    private GetProvinceQueryPort getProvinceQueryPort;
    @Mock
    private ListDistrictsQueryPort listDistrictsQueryPort;
    @Mock
    private GetDistrictQueryPort getDistrictQueryPort;
    @Mock
    private ListWardsQueryPort listWardsQueryPort;
    @Mock
    private GetWardQueryPort getWardQueryPort;
    @Mock
    private GeographyDtoMapper geographyDtoMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        GeographyController controller = new GeographyController(
                listCountriesQueryPort, getCountryQueryPort,
                listProvincesQueryPort, getProvinceQueryPort,
                listDistrictsQueryPort, getDistrictQueryPort,
                listWardsQueryPort, getWardQueryPort,
                geographyDtoMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new IdentityExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().build()))
                .build();
    }

    @Test
    void listCountriesReturnsAllCountries() throws Exception {
        GeographyResult vn = new GeographyResult("VN", "Viet Nam", "Vietnam");
        GeographyResult us = new GeographyResult("US", "Hoa Ky", "United States");
        List<GeographyResult> results = List.of(vn, us);

        GeographyResponse vnResp = new GeographyResponse("VN", "Viet Nam", "Vietnam");
        GeographyResponse usResp = new GeographyResponse("US", "Hoa Ky", "United States");

        when(listCountriesQueryPort.execute()).thenReturn(results);
        when(geographyDtoMapper.toResponses(results)).thenReturn(List.of(vnResp, usResp));

        mockMvc.perform(get("/api/v1/geography/countries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].code").value("VN"))
                .andExpect(jsonPath("$.data[0].name").value("Viet Nam"))
                .andExpect(jsonPath("$.data[1].code").value("US"));

        verify(listCountriesQueryPort).execute();
    }

    @Test
    void getCountryReturnsCountryByCode() throws Exception {
        GeographyResult result = new GeographyResult("VN", "Viet Nam", "Vietnam");
        GeographyResponse response = new GeographyResponse("VN", "Viet Nam", "Vietnam");

        when(getCountryQueryPort.execute("VN")).thenReturn(result);
        when(geographyDtoMapper.toResponse(result)).thenReturn(response);

        mockMvc.perform(get("/api/v1/geography/countries/VN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("VN"))
                .andExpect(jsonPath("$.data.name").value("Viet Nam"))
                .andExpect(jsonPath("$.data.nameEn").value("Vietnam"));

        verify(getCountryQueryPort).execute("VN");
    }

    @Test
    void listProvincesReturnsAllProvincesWhenNoCountryCode() throws Exception {
        GeographyResult hanoi = new GeographyResult("01", "Ha Noi", "Hanoi");
        GeographyResult hcm = new GeographyResult("79", "Ho Chi Minh", "Ho Chi Minh City");
        List<GeographyResult> results = List.of(hanoi, hcm);

        GeographyResponse hanoiResp = new GeographyResponse("01", "Ha Noi", "Hanoi");
        GeographyResponse hcmResp = new GeographyResponse("79", "Ho Chi Minh", "Ho Chi Minh City");

        when(listProvincesQueryPort.execute(null)).thenReturn(results);
        when(geographyDtoMapper.toResponses(results)).thenReturn(List.of(hanoiResp, hcmResp));

        mockMvc.perform(get("/api/v1/geography/provinces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].code").value("01"))
                .andExpect(jsonPath("$.data[1].code").value("79"));

        verify(listProvincesQueryPort).execute(null);
    }

    @Test
    void listProvincesFiltersByCountryCode() throws Exception {
        GeographyResult hanoi = new GeographyResult("01", "Ha Noi", "Hanoi");
        List<GeographyResult> results = List.of(hanoi);

        GeographyResponse hanoiResp = new GeographyResponse("01", "Ha Noi", "Hanoi");

        when(listProvincesQueryPort.execute("VN")).thenReturn(results);
        when(geographyDtoMapper.toResponses(results)).thenReturn(List.of(hanoiResp));

        mockMvc.perform(get("/api/v1/geography/provinces").param("countryCode", "VN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("01"));

        verify(listProvincesQueryPort).execute("VN");
    }

    @Test
    void getProvinceReturnsProvinceByCode() throws Exception {
        GeographyResult result = new GeographyResult("01", "Ha Noi", "Hanoi");
        GeographyResponse response = new GeographyResponse("01", "Ha Noi", "Hanoi");

        when(getProvinceQueryPort.execute("01")).thenReturn(result);
        when(geographyDtoMapper.toResponse(result)).thenReturn(response);

        mockMvc.perform(get("/api/v1/geography/provinces/01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("01"))
                .andExpect(jsonPath("$.data.name").value("Ha Noi"));

        verify(getProvinceQueryPort).execute("01");
    }

    @Test
    void listDistrictsReturnsDistrictsByProvince() throws Exception {
        GeographyResult district = new GeographyResult("001", "Ba Dinh", "Ba Dinh");
        List<GeographyResult> results = List.of(district);
        GeographyResponse response = new GeographyResponse("001", "Ba Dinh", "Ba Dinh");

        when(listDistrictsQueryPort.execute("01")).thenReturn(results);
        when(geographyDtoMapper.toResponses(results)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/geography/districts").param("provinceCode", "01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("001"))
                .andExpect(jsonPath("$.data[0].name").value("Ba Dinh"));

        verify(listDistrictsQueryPort).execute("01");
    }

    @Test
    void getDistrictReturnsDistrictByCode() throws Exception {
        GeographyResult result = new GeographyResult("001", "Ba Dinh", "Ba Dinh");
        GeographyResponse response = new GeographyResponse("001", "Ba Dinh", "Ba Dinh");

        when(getDistrictQueryPort.execute("001")).thenReturn(result);
        when(geographyDtoMapper.toResponse(result)).thenReturn(response);

        mockMvc.perform(get("/api/v1/geography/districts/001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("001"));

        verify(getDistrictQueryPort).execute("001");
    }

    @Test
    void listWardsReturnsWardsByDistrict() throws Exception {
        GeographyResult ward = new GeographyResult("00001", "Cong Vi", "Cong Vi");
        List<GeographyResult> results = List.of(ward);
        GeographyResponse response = new GeographyResponse("00001", "Cong Vi", "Cong Vi");

        when(listWardsQueryPort.execute("001")).thenReturn(results);
        when(geographyDtoMapper.toResponses(results)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/geography/wards").param("districtCode", "001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("00001"))
                .andExpect(jsonPath("$.data[0].name").value("Cong Vi"));

        verify(listWardsQueryPort).execute("001");
    }

    @Test
    void getWardReturnsWardByCode() throws Exception {
        GeographyResult result = new GeographyResult("00001", "Cong Vi", "Cong Vi");
        GeographyResponse response = new GeographyResponse("00001", "Cong Vi", "Cong Vi");

        when(getWardQueryPort.execute("00001")).thenReturn(result);
        when(geographyDtoMapper.toResponse(result)).thenReturn(response);

        mockMvc.perform(get("/api/v1/geography/wards/00001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("00001"));

        verify(getWardQueryPort).execute("00001");
    }
}
