package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.dto.brand.CreateBrandRequest;
import com.aionn.catalog.adapter.rest.dto.brand.DeleteBrandRequest;
import com.aionn.catalog.adapter.rest.dto.brand.UpdateBrandRequest;
import com.aionn.catalog.adapter.rest.exception.CatalogExceptionHandler;
import com.aionn.catalog.adapter.rest.mapper.brand.BrandDtoMapperImpl;
import com.aionn.catalog.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.catalog.adapter.rest.support.TestAuth;
import com.aionn.catalog.application.dto.brand.command.CreateBrandCommand;
import com.aionn.catalog.application.dto.brand.command.DeleteBrandCommand;
import com.aionn.catalog.application.dto.brand.command.UpdateBrandCommand;
import com.aionn.catalog.application.dto.brand.query.GetBrandQuery;
import com.aionn.catalog.application.dto.brand.query.ListBrandsQuery;
import com.aionn.catalog.application.dto.brand.result.BrandResult;
import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.port.in.brand.CreateBrandInputPort;
import com.aionn.catalog.application.port.in.brand.DeleteBrandInputPort;
import com.aionn.catalog.application.port.in.brand.GetBrandInputPort;
import com.aionn.catalog.application.port.in.brand.ListBrandsInputPort;
import com.aionn.catalog.application.port.in.brand.UpdateBrandInputPort;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BrandControllerWebTest {

    private static final String BRAND_ID = "01HZBRD0000000000000000001";

    @Mock
    private CreateBrandInputPort createBrandInputPort;
    @Mock
    private UpdateBrandInputPort updateBrandInputPort;
    @Mock
    private DeleteBrandInputPort deleteBrandInputPort;
    @Mock
    private ListBrandsInputPort listBrandsInputPort;
    @Mock
    private GetBrandInputPort getBrandInputPort;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @BeforeEach
    void setUp() {
        BrandController controller = new BrandController(
                createBrandInputPort,
                updateBrandInputPort,
                deleteBrandInputPort,
                listBrandsInputPort,
                getBrandInputPort,
                new BrandDtoMapperImpl());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new CatalogExceptionHandler())
                .addInterceptors(new MockSecurityInterceptor())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private BrandResult sample() {
        return new BrandResult(BRAND_ID, "Acme", null, "desc", "ACTIVE", Instant.now(), Instant.now());
    }

    @Test
    void createReturnsCreated() throws Exception {
        when(createBrandInputPort.execute(any(CreateBrandCommand.class))).thenReturn(sample());

        mockMvc.perform(post("/api/v1/catalog/brands")
                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateBrandRequest("Acme", null, "desc"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.brandId").value(BRAND_ID));

        verify(createBrandInputPort).execute(any(CreateBrandCommand.class));
    }

    @Test
    void updateReturnsOk() throws Exception {
        when(updateBrandInputPort.execute(any(UpdateBrandCommand.class))).thenReturn(sample());

        mockMvc.perform(put("/api/v1/catalog/brands/" + BRAND_ID)
                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateBrandRequest("Acme New", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.brandId").value(BRAND_ID));

        verify(updateBrandInputPort).execute(any(UpdateBrandCommand.class));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/v1/catalog/brands/" + BRAND_ID + "/delete")
                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DeleteBrandRequest("policy"))))
                .andExpect(status().isNoContent());

        verify(deleteBrandInputPort).execute(any(DeleteBrandCommand.class));
    }

    @Test
    void getReturnsBrand() throws Exception {
        when(getBrandInputPort.execute(any(GetBrandQuery.class))).thenReturn(sample());

        mockMvc.perform(get("/api/v1/catalog/brands/" + BRAND_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.brandId").value(BRAND_ID));
    }

    @Test
    void getReturnsNotFoundWhenServiceThrows() throws Exception {
        when(getBrandInputPort.execute(any(GetBrandQuery.class)))
                .thenThrow(new CatalogException(CatalogErrorCode.BRAND_NOT_FOUND));

        mockMvc.perform(get("/api/v1/catalog/brands/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listReturnsPagedBrands() throws Exception {
        PageResult<BrandResult> page = new PageResult<>(List.of(sample()), 0, 20, 1);
        when(listBrandsInputPort.execute(any(ListBrandsQuery.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/catalog/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].brandId").value(BRAND_ID));

        verify(listBrandsInputPort).execute(any(ListBrandsQuery.class));
    }
}
