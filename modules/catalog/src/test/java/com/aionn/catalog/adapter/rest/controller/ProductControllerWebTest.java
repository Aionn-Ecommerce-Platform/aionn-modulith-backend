package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.dto.product.AssignBrandRequest;
import com.aionn.catalog.adapter.rest.dto.product.AssignCategoriesRequest;
import com.aionn.catalog.adapter.rest.dto.product.BulkPriceUpdateRequest;
import com.aionn.catalog.adapter.rest.dto.product.ChangeVariantPriceRequest;
import com.aionn.catalog.adapter.rest.dto.product.CreateProductRequest;
import com.aionn.catalog.adapter.rest.dto.product.DeactivateProductRequest;
import com.aionn.catalog.adapter.rest.dto.product.DefineAttributesRequest;
import com.aionn.catalog.adapter.rest.dto.product.DefineVariantRequest;
import com.aionn.catalog.adapter.rest.dto.product.RejectProductRequest;
import com.aionn.catalog.adapter.rest.exception.CatalogExceptionHandler;
import com.aionn.catalog.adapter.rest.mapper.product.ProductDtoMapperImpl;
import com.aionn.catalog.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.catalog.adapter.rest.support.TestAuth;
import com.aionn.catalog.adapter.rest.support.session.CurrentAdminIdArgumentResolver;
import com.aionn.catalog.adapter.rest.support.session.CurrentMerchantIdArgumentResolver;
import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.product.command.AssignBrandCommand;
import com.aionn.catalog.application.dto.product.command.AssignCategoriesCommand;
import com.aionn.catalog.application.dto.product.command.BulkPriceUpdateCommand;
import com.aionn.catalog.application.dto.product.command.ChangeVariantPriceCommand;
import com.aionn.catalog.application.dto.product.command.CloneProductCommand;
import com.aionn.catalog.application.dto.product.command.CreateProductCommand;
import com.aionn.catalog.application.dto.product.command.DeactivateProductCommand;
import com.aionn.catalog.application.dto.product.command.DefineAttributesCommand;
import com.aionn.catalog.application.dto.product.command.DefineVariantCommand;
import com.aionn.catalog.application.dto.product.command.PublishProductCommand;
import com.aionn.catalog.application.dto.product.command.RejectProductCommand;
import com.aionn.catalog.application.dto.product.command.RestoreProductCommand;
import com.aionn.catalog.application.dto.product.command.SubmitForReviewCommand;
import com.aionn.catalog.application.dto.product.query.GetProductQuery;
import com.aionn.catalog.application.dto.product.query.GetProductsBySkuIdsQuery;
import com.aionn.catalog.application.dto.product.query.ListProductsByMerchantQuery;
import com.aionn.catalog.application.dto.product.query.ListProductsByStatusQuery;
import com.aionn.catalog.application.dto.product.result.BulkPriceUpdateResult;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.AssignBrandInputPort;
import com.aionn.catalog.application.port.in.product.AssignCategoriesInputPort;
import com.aionn.catalog.application.port.in.product.BulkPriceUpdateInputPort;
import com.aionn.catalog.application.port.in.product.ChangeVariantPriceInputPort;
import com.aionn.catalog.application.port.in.product.CloneProductInputPort;
import com.aionn.catalog.application.port.in.product.CreateProductInputPort;
import com.aionn.catalog.application.port.in.product.DeactivateProductInputPort;
import com.aionn.catalog.application.port.in.product.DefineAttributesInputPort;
import com.aionn.catalog.application.port.in.product.DefineVariantInputPort;
import com.aionn.catalog.application.port.in.product.GetProductInputPort;
import com.aionn.catalog.application.port.in.product.GetProductsBySkuIdsInputPort;
import com.aionn.catalog.application.port.in.product.ListProductsByMerchantInputPort;
import com.aionn.catalog.application.port.in.product.ListProductsByStatusInputPort;
import com.aionn.catalog.application.port.in.product.PublishProductInputPort;
import com.aionn.catalog.application.port.in.product.RejectProductInputPort;
import com.aionn.catalog.application.port.in.product.RestoreProductInputPort;
import com.aionn.catalog.application.port.in.product.SubmitForReviewInputPort;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerWebTest {

        private static final String PRODUCT_ID = "01HZPRD0000000000000000001";
        private static final String MERCHANT_ID = "01HZMER0000000000000000001";

        @Mock
        private CreateProductInputPort createProductInputPort;
        @Mock
        private CloneProductInputPort cloneProductInputPort;
        @Mock
        private DefineVariantInputPort defineVariantInputPort;
        @Mock
        private ChangeVariantPriceInputPort changeVariantPriceInputPort;
        @Mock
        private BulkPriceUpdateInputPort bulkPriceUpdateInputPort;
        @Mock
        private AssignBrandInputPort assignBrandInputPort;
        @Mock
        private AssignCategoriesInputPort assignCategoriesInputPort;
        @Mock
        private DefineAttributesInputPort defineAttributesInputPort;
        @Mock
        private PublishProductInputPort publishProductInputPort;
        @Mock
        private SubmitForReviewInputPort submitForReviewInputPort;
        @Mock
        private RejectProductInputPort rejectProductInputPort;
        @Mock
        private DeactivateProductInputPort deactivateProductInputPort;
        @Mock
        private RestoreProductInputPort restoreProductInputPort;
        @Mock
        private GetProductInputPort getProductInputPort;
        @Mock
        private GetProductsBySkuIdsInputPort getProductsBySkuIdsInputPort;
        @Mock
        private ListProductsByMerchantInputPort listProductsByMerchantInputPort;
        @Mock
        private ListProductsByStatusInputPort listProductsByStatusInputPort;
        @Mock
        private com.aionn.catalog.application.port.in.product.RemoveVariantInputPort removeVariantInputPort;
        @Mock
        private com.aionn.catalog.application.port.in.product.UpdateMediaInputPort updateMediaInputPort;
        @Mock
        private com.aionn.catalog.application.port.in.product.UpdateAiMetadataInputPort updateAiMetadataInputPort;
        @Mock
        private com.aionn.catalog.application.port.in.product.AssignCollectionsInputPort assignCollectionsInputPort;
        @Mock
        private com.aionn.catalog.application.port.in.product.EmergencyTakedownInputPort emergencyTakedownInputPort;
        @Mock
        private com.aionn.catalog.application.port.in.product.SearchProductsInputPort searchProductsInputPort;

        private MockMvc mockMvc;
        private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

        @BeforeEach
        void setUp() {
                ProductController controller = new ProductController(
                                createProductInputPort, cloneProductInputPort, defineVariantInputPort,
                                changeVariantPriceInputPort, bulkPriceUpdateInputPort, assignBrandInputPort,
                                assignCategoriesInputPort, defineAttributesInputPort, publishProductInputPort,
                                submitForReviewInputPort, rejectProductInputPort, deactivateProductInputPort,
                                restoreProductInputPort, getProductInputPort, getProductsBySkuIdsInputPort,
                                listProductsByMerchantInputPort, listProductsByStatusInputPort,
                                removeVariantInputPort, updateMediaInputPort, updateAiMetadataInputPort,
                                assignCollectionsInputPort, emergencyTakedownInputPort, searchProductsInputPort,
                                new ProductDtoMapperImpl());
                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new CatalogExceptionHandler())
                                .setCustomArgumentResolvers(
                                                new CurrentMerchantIdArgumentResolver(
                                                                new com.aionn.catalog.adapter.rest.support.MockOwnershipVerifier()),
                                                new CurrentAdminIdArgumentResolver())
                                .addInterceptors(new MockSecurityInterceptor())
                                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                                .build();
        }

        private ProductResult sample() {
                return new ProductResult(PRODUCT_ID, MERCHANT_ID, "Widget", null,
                                List.of(), List.of(), List.of(), Map.of(),
                                List.of(), null, "DRAFT", Instant.now(), Instant.now());
        }

        @Test
        void createReturnsCreated() throws Exception {
                when(createProductInputPort.execute(any(CreateProductCommand.class))).thenReturn(sample());

                mockMvc.perform(post("/api/v1/catalog/products")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new CreateProductRequest("Widget"))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.productId").value(PRODUCT_ID));
        }

        @Test
        void cloneReturnsCreated() throws Exception {
                when(cloneProductInputPort.execute(any(CloneProductCommand.class))).thenReturn(sample());

                mockMvc.perform(post("/api/v1/catalog/products/" + PRODUCT_ID + "/clone")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID)))
                                .andExpect(status().isCreated());
        }

        @Test
        void defineVariantReturnsOk() throws Exception {
                when(defineVariantInputPort.execute(any(DefineVariantCommand.class))).thenReturn(sample());

                mockMvc.perform(post("/api/v1/catalog/products/" + PRODUCT_ID + "/variants")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new DefineVariantRequest(
                                                "sku-1", Map.of("color", "red"), new BigDecimal("10.00"), "VND"))))
                                .andExpect(status().isOk());
        }

        @Test
        void changeVariantPriceReturnsOk() throws Exception {
                when(changeVariantPriceInputPort.execute(any(ChangeVariantPriceCommand.class))).thenReturn(sample());

                mockMvc.perform(put("/api/v1/catalog/products/" + PRODUCT_ID + "/variants/sku-1/price")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new ChangeVariantPriceRequest(
                                                new BigDecimal("20.00"), "VND"))))
                                .andExpect(status().isOk());
        }

        @Test
        void bulkPriceUpdateReturnsOk() throws Exception {
                when(bulkPriceUpdateInputPort.execute(any(BulkPriceUpdateCommand.class)))
                                .thenReturn(new BulkPriceUpdateResult(1, 0, List.of()));

                mockMvc.perform(post("/api/v1/catalog/products/bulk-price")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new BulkPriceUpdateRequest(List.of(
                                                new BulkPriceUpdateRequest.Item(PRODUCT_ID, "sku-1",
                                                                new BigDecimal("20"), "VND"))))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.updated").value(1));
        }

        @Test
        void assignBrandReturnsOk() throws Exception {
                when(assignBrandInputPort.execute(any(AssignBrandCommand.class))).thenReturn(sample());

                mockMvc.perform(put("/api/v1/catalog/products/" + PRODUCT_ID + "/brand")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new AssignBrandRequest("brand-1"))))
                                .andExpect(status().isOk());
        }

        @Test
        void assignCategoriesReturnsOk() throws Exception {
                when(assignCategoriesInputPort.execute(any(AssignCategoriesCommand.class))).thenReturn(sample());

                mockMvc.perform(put("/api/v1/catalog/products/" + PRODUCT_ID + "/categories")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new AssignCategoriesRequest(List.of("cat-1")))))
                                .andExpect(status().isOk());
        }

        @Test
        void defineAttributesReturnsOk() throws Exception {
                when(defineAttributesInputPort.execute(any(DefineAttributesCommand.class))).thenReturn(sample());

                mockMvc.perform(put("/api/v1/catalog/products/" + PRODUCT_ID + "/attributes")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new DefineAttributesRequest(Map.of("material", "silk")))))
                                .andExpect(status().isOk());
        }

        @Test
        void submitForReviewReturnsOk() throws Exception {
                when(submitForReviewInputPort.execute(any(SubmitForReviewCommand.class))).thenReturn(sample());

                mockMvc.perform(post("/api/v1/catalog/products/" + PRODUCT_ID + "/submit-review")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID)))
                                .andExpect(status().isOk());
        }

        @Test
        void publishReturnsOk() throws Exception {
                when(publishProductInputPort.execute(any(PublishProductCommand.class))).thenReturn(sample());

                mockMvc.perform(post("/api/v1/catalog/products/" + PRODUCT_ID + "/publish")
                                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN")))
                                .andExpect(status().isOk());
        }

        @Test
        void rejectReturnsOk() throws Exception {
                when(rejectProductInputPort.execute(any(RejectProductCommand.class))).thenReturn(sample());

                mockMvc.perform(post("/api/v1/catalog/products/" + PRODUCT_ID + "/reject")
                                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper
                                                .writeValueAsString(new RejectProductRequest("IMG_ISSUE", "note"))))
                                .andExpect(status().isOk());
        }

        @Test
        void deactivateReturnsOk() throws Exception {
                when(deactivateProductInputPort.execute(any(DeactivateProductCommand.class))).thenReturn(sample());

                mockMvc.perform(post("/api/v1/catalog/products/" + PRODUCT_ID + "/deactivate")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new DeactivateProductRequest("policy"))))
                                .andExpect(status().isOk());
        }

        @Test
        void restoreReturnsOk() throws Exception {
                when(restoreProductInputPort.execute(any(RestoreProductCommand.class))).thenReturn(sample());

                mockMvc.perform(post("/api/v1/catalog/products/" + PRODUCT_ID + "/restore")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID)))
                                .andExpect(status().isOk());
        }

        @Test
        void getReturnsOk() throws Exception {
                when(getProductInputPort.execute(any(GetProductQuery.class))).thenReturn(sample());

                mockMvc.perform(get("/api/v1/catalog/products/" + PRODUCT_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.productId").value(PRODUCT_ID));
        }

        @Test
        void getReturnsNotFoundWhenServiceThrows() throws Exception {
                when(getProductInputPort.execute(any(GetProductQuery.class)))
                                .thenThrow(new CatalogException(CatalogErrorCode.PRODUCT_NOT_FOUND));

                mockMvc.perform(get("/api/v1/catalog/products/missing"))
                                .andExpect(status().isNotFound());
        }

        @Test
        void getBySkuIdsReturnsOk() throws Exception {
                when(getProductsBySkuIdsInputPort.execute(any(GetProductsBySkuIdsQuery.class)))
                                .thenReturn(List.of(sample()));

                mockMvc.perform(get("/api/v1/catalog/products/by-sku").param("skuIds", "sku-1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].productId").value(PRODUCT_ID));
        }

        @Test
        void listByMerchantReturnsOk() throws Exception {
                when(listProductsByMerchantInputPort.execute(any(ListProductsByMerchantQuery.class)))
                                .thenReturn(new PageResult<>(List.of(sample()), 0, 20, 1));

                mockMvc.perform(get("/api/v1/catalog/products").param("merchantId", MERCHANT_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].productId").value(PRODUCT_ID));

                verify(listProductsByMerchantInputPort).execute(any(ListProductsByMerchantQuery.class));
        }

        @Test
        void listByStatusReturnsOk() throws Exception {
                when(listProductsByStatusInputPort.execute(any(ListProductsByStatusQuery.class)))
                                .thenReturn(new PageResult<>(List.of(sample()), 0, 20, 1));

                mockMvc.perform(get("/api/v1/catalog/products").param("status", "PUBLISHED"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].productId").value(PRODUCT_ID));

                verify(listProductsByStatusInputPort).execute(any(ListProductsByStatusQuery.class));
        }

        @Test
        void removeVariantReturnsOk() throws Exception {
                when(removeVariantInputPort.execute(
                                any(com.aionn.catalog.application.dto.product.command.RemoveVariantCommand.class)))
                                .thenReturn(sample());

                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/v1/catalog/products/" + PRODUCT_ID + "/variants/sku-1")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID)))
                                .andExpect(status().isOk());
        }

        @Test
        void updateMediaReturnsOk() throws Exception {
                when(updateMediaInputPort.execute(
                                any(com.aionn.catalog.application.dto.product.command.UpdateMediaCommand.class)))
                                .thenReturn(sample());

                mockMvc.perform(put("/api/v1/catalog/products/" + PRODUCT_ID + "/media")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new com.aionn.catalog.adapter.rest.dto.product.UpdateMediaRequest(
                                                                java.util.List.of("img1", "img2")))))
                                .andExpect(status().isOk());
        }

        @Test
        void updateAiMetadataReturnsOk() throws Exception {
                when(updateAiMetadataInputPort.execute(
                                any(com.aionn.catalog.application.dto.product.command.UpdateAiMetadataCommand.class)))
                                .thenReturn(sample());

                mockMvc.perform(put("/api/v1/catalog/products/" + PRODUCT_ID + "/ai-metadata")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new com.aionn.catalog.adapter.rest.dto.product.UpdateAiMetadataRequest(
                                                                java.util.List.of("premium"), "handmade"))))
                                .andExpect(status().isOk());
        }

        @Test
        void assignCollectionsReturnsOk() throws Exception {
                when(assignCollectionsInputPort.execute(
                                any(com.aionn.catalog.application.dto.product.command.AssignCollectionsCommand.class)))
                                .thenReturn(sample());

                mockMvc.perform(put("/api/v1/catalog/products/" + PRODUCT_ID + "/collections")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new com.aionn.catalog.adapter.rest.dto.product.AssignCollectionsRequest(
                                                                java.util.List.of("coll-1")))))
                                .andExpect(status().isOk());
        }

        @Test
        void emergencyTakedownReturnsOk() throws Exception {
                when(emergencyTakedownInputPort.execute(
                                any(com.aionn.catalog.application.dto.product.command.EmergencyTakedownCommand.class)))
                                .thenReturn(sample());

                mockMvc.perform(post("/api/v1/catalog/products/" + PRODUCT_ID + "/takedown")
                                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new com.aionn.catalog.adapter.rest.dto.product.EmergencyTakedownRequest(
                                                                "abuse"))))
                                .andExpect(status().isOk());
        }

        @Test
        void searchReturnsOk() throws Exception {
                when(searchProductsInputPort.execute(
                                any(com.aionn.catalog.application.dto.product.query.SearchProductsQuery.class)))
                                .thenReturn(new PageResult<>(java.util.List.of(sample()), 0, 20, 1));

                mockMvc.perform(get("/api/v1/catalog/products/search").param("keyword", "widget"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].productId").value(PRODUCT_ID));
        }
}
