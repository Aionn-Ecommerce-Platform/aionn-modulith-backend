package com.aionn.ucp.adapter.rest.controller;

import com.aionn.ucp.adapter.rest.advice.UcpControllerAdvice;
import com.aionn.ucp.adapter.rest.dto.cart.UcpResponseMetadata;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpCatalogLookupResponse;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpCatalogModels.*;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpCatalogSearchResponse;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpProductDetailResponse;
import com.aionn.ucp.application.catalog.UcpCatalogApplicationService;
import com.aionn.ucp.domain.exception.UcpProtocolException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UcpCatalogControllerTest {

        @Mock
        private UcpCatalogApplicationService catalogService;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders
                                .standaloneSetup(new UcpCatalogController(catalogService))
                                .setControllerAdvice(new UcpControllerAdvice())
                                .build();
        }

        private UcpProductDto sampleProductDto(String id, String title) {
                UcpPriceDto price = new UcpPriceDto(2000L, "USD");
                UcpVariantDto variant = new UcpVariantDto(
                                id + "-sku",
                                title + " Variant",
                                new UcpDescriptionDto(title),
                                price,
                                id + "-sku",
                                new UcpAvailabilityDto(true, "in_stock"),
                                null, null,
                                List.of(new UcpInputCorrelationDto(id, "exact")));

                return new UcpProductDto(
                                id,
                                title,
                                new UcpDescriptionDto(title),
                                new UcpPriceRangeDto(price, price),
                                List.of(variant),
                                null, null, null, null, null, null, null, null);
        }

        @Test
        void searchReturnsOkWithSearchResults() throws Exception {
                UcpCatalogSearchResponse searchResponse = new UcpCatalogSearchResponse(
                                UcpResponseMetadata.success("2026-08-25"),
                                List.of(sampleProductDto("prod-1", "Shoes")),
                                new UcpPaginationResponseDto(false, null, 1),
                                null, null, null);

                when(catalogService.searchCatalog(any())).thenReturn(searchResponse);

                mockMvc.perform(post("/ucp/v1/catalog/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "query": "shoes",
                                                  "pagination": { "limit": 10 }
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.ucp.version").value("2026-08-25"))
                                .andExpect(jsonPath("$.products[0].id").value("prod-1"))
                                .andExpect(jsonPath("$.products[0].title").value("Shoes"))
                                .andExpect(jsonPath("$.pagination.has_next_page").value(false))
                                .andExpect(jsonPath("$.pagination.total_count").value(1));
        }

        @Test
        void lookupReturnsOkWithResolvedProducts() throws Exception {
                UcpCatalogLookupResponse lookupResponse = new UcpCatalogLookupResponse(
                                UcpResponseMetadata.success("2026-08-25"),
                                List.of(sampleProductDto("prod-1", "Shoes")),
                                null, null, null);

                when(catalogService.lookupCatalog(any())).thenReturn(lookupResponse);

                mockMvc.perform(post("/ucp/v1/catalog/lookup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "ids": ["prod-1"]
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.ucp.version").value("2026-08-25"))
                                .andExpect(jsonPath("$.products[0].id").value("prod-1"));
        }

        @Test
        void getProductReturnsOkWithProductDetail() throws Exception {
                UcpProductDetailResponse detailResponse = new UcpProductDetailResponse(
                                UcpResponseMetadata.success("2026-08-25"),
                                sampleProductDto("prod-1", "Shoes"),
                                null, null, null);

                when(catalogService.getProduct(any())).thenReturn(detailResponse);

                mockMvc.perform(post("/ucp/v1/catalog/product")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "id": "prod-1"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.ucp.version").value("2026-08-25"))
                                .andExpect(jsonPath("$.product.id").value("prod-1"));
        }

        @Test
        void getProductReturnsNotFoundWhenMissing() throws Exception {
                when(catalogService.getProduct(any()))
                                .thenThrow(new UcpProtocolException(404, "item_not_found", "Product not found: missing",
                                                "error", "$.id"));

                mockMvc.perform(post("/ucp/v1/catalog/product")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "id": "missing"
                                                }
                                                """))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.messages[0].code").value("item_not_found"))
                                .andExpect(jsonPath("$.messages[0].path").value("$.id"));
        }
}
