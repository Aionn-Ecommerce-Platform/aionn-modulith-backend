package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.exception.CatalogExceptionHandler;
import com.aionn.catalog.adapter.rest.mapper.collection.CollectionDtoMapper;
import com.aionn.catalog.adapter.rest.support.MockSecurityInterceptor;import com.aionn.catalog.adapter.rest.support.TestAuth;
import com.aionn.catalog.application.port.out.merchant.CollectionPersistencePort.CollectionView;
import com.aionn.catalog.application.service.CollectionService;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.platform.infrastructure.config.JacksonMapperFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CollectionControllerWebTest {

    private static final String COLLECTION_ID = "col_1";
    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");

    @Mock
    private CollectionService collectionService;

    private MockMvc mockMvc;
    private final JsonMapper objectMapper = JacksonMapperFactory.create();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CollectionController(collectionService,
                Mappers.getMapper(CollectionDtoMapper.class)))
                .setControllerAdvice(new CatalogExceptionHandler())
                .addInterceptors(new MockSecurityInterceptor())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void listReturnsCollections() throws Exception {
        when(collectionService.list(true)).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/v1/catalog/collections").param("activeOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].collectionId").value(COLLECTION_ID));
    }

    @Test
    void getReturnsACollectionBySlug() throws Exception {
        when(collectionService.get("summer")).thenReturn(sample());

        mockMvc.perform(get("/api/v1/catalog/collections/summer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("summer"));
    }

    @Test
    void getReturnsNotFoundForAnUnknownCollection() throws Exception {
        when(collectionService.get("missing"))
                .thenThrow(new CatalogException(CatalogErrorCode.COLLECTION_NOT_FOUND));

        mockMvc.perform(get("/api/v1/catalog/collections/missing"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void createPassesNullDefaultsToTheService() throws Exception {
        when(collectionService.create(eq("summer"), eq("Summer"), any(), any(), isNull(), isNull()))
                .thenReturn(sample());

        mockMvc.perform(post("/api/v1/catalog/collections")
                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"slug":"summer","name":"Summer"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.collectionId").value(COLLECTION_ID));
    }

    @Test
    void updateUsesTheProvidedValues() throws Exception {
        when(collectionService.update(eq(COLLECTION_ID), eq("summer"), eq("Summer"), any(), any(),
                eq(false), eq(3))).thenReturn(sample());

        mockMvc.perform(put("/api/v1/catalog/collections/" + COLLECTION_ID)
                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"slug":"summer","name":"Summer","active":false,"displayOrder":3}
                        """))
                .andExpect(status().isOk());
    }

    @Test
    void deleteRemovesTheCollection() throws Exception {
        mockMvc.perform(delete("/api/v1/catalog/collections/" + COLLECTION_ID)
                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN")))
                .andExpect(status().isOk());

        verify(collectionService).delete(COLLECTION_ID);
    }

    private static CollectionView sample() {
        return new CollectionView(COLLECTION_ID, "summer", "Summer", "desc", "img", true, 0, NOW, NOW);
    }
}
