package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.exception.CatalogExceptionHandler;
import com.aionn.catalog.adapter.rest.mapper.media.CatalogMediaDtoMapperImpl;
import com.aionn.catalog.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.catalog.adapter.rest.support.TestAuth;
import com.aionn.catalog.adapter.rest.support.session.CurrentOwnerIdArgumentResolver;
import com.aionn.catalog.application.dto.media.result.UploadSignatureResult;
import com.aionn.catalog.application.port.in.media.GenerateProductMediaUploadSignatureInputPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CatalogMediaControllerWebTest {

    @Mock
    private GenerateProductMediaUploadSignatureInputPort generateProductMediaUploadSignatureInputPort;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @BeforeEach
    void setUp() {
        CatalogMediaController controller = new CatalogMediaController(
                generateProductMediaUploadSignatureInputPort,
                new CatalogMediaDtoMapperImpl());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new CatalogExceptionHandler())
                .setCustomArgumentResolvers(new CurrentOwnerIdArgumentResolver())
                .addInterceptors(new MockSecurityInterceptor())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private UploadSignatureResult sample() {
        return new UploadSignatureResult(
                "sig-1", "1700000000", "api-key", "cloud-name",
                "https://api.cloudinary.com/v1_1/cloud-name/image/upload", "catalog/products/owner-1");
    }

    @Test
    void productImageSignatureUsesOwnerPrincipal() throws Exception {
        when(generateProductMediaUploadSignatureInputPort.execute("owner-1")).thenReturn(sample());

        mockMvc.perform(post("/api/v1/catalog/media/upload-signatures/product-image")
                .with(TestAuth.authUser("owner-1", "MERCHANT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.signature").value("sig-1"))
                .andExpect(jsonPath("$.data.cloudName").value("cloud-name"));

        verify(generateProductMediaUploadSignatureInputPort).execute("owner-1");
    }

    @Test
    void reviewImageSignatureUsesOwnerPrincipal() throws Exception {
        when(generateProductMediaUploadSignatureInputPort.executeReview("owner-1")).thenReturn(sample());

        mockMvc.perform(post("/api/v1/catalog/media/upload-signatures/review-image")
                .with(TestAuth.authUser("owner-1", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.folder").value("catalog/products/owner-1"));

        verify(generateProductMediaUploadSignatureInputPort).executeReview("owner-1");
    }
}
