package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.dto.product.request.AssignCollectionsRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.EmergencyTakedownRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.UpdateAiMetadataRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.UpdateMediaRequest;
import com.aionn.catalog.adapter.rest.support.MockOwnershipVerifier;
import com.aionn.catalog.application.dto.analytics.result.ProductAnalyticsResult;
import com.aionn.catalog.application.dto.product.command.AssignCollectionsCommand;
import com.aionn.catalog.application.dto.product.command.EmergencyTakedownCommand;
import com.aionn.catalog.application.dto.product.command.RemoveVariantCommand;
import com.aionn.catalog.application.dto.product.command.TrackProductViewCommand;
import com.aionn.catalog.application.dto.product.command.UpdateAiMetadataCommand;
import com.aionn.catalog.application.dto.product.command.UpdateMediaCommand;
import com.aionn.catalog.application.dto.product.query.GetPersonalizedProductsQuery;
import com.aionn.catalog.application.dto.product.query.GetPopularProductsQuery;
import com.aionn.catalog.application.dto.product.query.GetRelatedProductsQuery;
import com.aionn.catalog.application.dto.product.query.SearchProductsQuery;
import com.aionn.catalog.application.dto.search.ProductSearchCriteria;
import com.aionn.catalog.application.dto.search.ProductSearchResult;
import com.aionn.catalog.application.port.in.product.AssignCollectionsInputPort;
import com.aionn.catalog.application.port.in.product.ChangeVariantParcelInputPort;
import com.aionn.catalog.application.port.in.product.EmergencyTakedownInputPort;
import com.aionn.catalog.application.port.in.product.GetPersonalizedProductsInputPort;
import com.aionn.catalog.application.port.in.product.GetPopularProductsInputPort;
import com.aionn.catalog.application.port.in.product.GetProductAnalyticsInputPort;
import com.aionn.catalog.application.port.in.product.GetRelatedProductsInputPort;
import com.aionn.catalog.application.port.in.product.RemoveVariantInputPort;
import com.aionn.catalog.application.port.in.product.SearchProductCatalogInputPort;
import com.aionn.catalog.application.port.in.product.SearchProductsInputPort;
import com.aionn.catalog.application.port.in.product.TrackProductViewInputPort;
import com.aionn.catalog.adapter.rest.mapper.product.ProductSearchCriteriaAssembler;
import com.aionn.catalog.application.port.in.product.UpdateAiMetadataInputPort;
import com.aionn.catalog.application.port.in.product.UpdateMediaInputPort;

import com.aionn.catalog.adapter.rest.mapper.product.ProductDtoMapper;

import org.mapstruct.factory.Mappers;

import com.aionn.catalog.adapter.rest.dto.product.request.AssignBrandRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.AssignCategoriesRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.BulkPriceUpdateRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.ChangeVariantParcelRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.ChangeVariantPriceRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.CreateProductRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.DeactivateProductRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.DefineAttributesRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.DefineVariantRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.RejectProductRequest;
import com.aionn.catalog.adapter.rest.exception.CatalogExceptionHandler;
import com.aionn.catalog.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.catalog.adapter.rest.support.TestAuth;
import com.aionn.catalog.adapter.rest.support.session.CurrentAdminIdArgumentResolver;
import com.aionn.catalog.adapter.rest.support.session.CurrentMerchantIdArgumentResolver;
import com.aionn.catalog.adapter.rest.support.session.CurrentOwnerIdArgumentResolver;
import com.aionn.catalog.adapter.rest.support.session.OptionalOwnerIdArgumentResolver;
import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.product.command.AssignBrandCommand;
import com.aionn.catalog.application.dto.product.command.AssignCategoriesCommand;
import com.aionn.catalog.application.dto.product.command.BulkPriceUpdateCommand;
import com.aionn.catalog.application.dto.product.command.ChangeVariantParcelCommand;
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
import com.aionn.platform.infrastructure.config.JacksonMapperFactory;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
        private ChangeVariantParcelInputPort changeVariantParcelInputPort;
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
        private RemoveVariantInputPort removeVariantInputPort;
        @Mock
        private UpdateMediaInputPort updateMediaInputPort;
        @Mock
        private UpdateAiMetadataInputPort updateAiMetadataInputPort;
        @Mock
        private AssignCollectionsInputPort assignCollectionsInputPort;
        @Mock
        private EmergencyTakedownInputPort emergencyTakedownInputPort;
        @Mock
        private SearchProductsInputPort searchProductsInputPort;
        @Mock
        private GetRelatedProductsInputPort getRelatedProductsInputPort;
        @Mock
        private GetPopularProductsInputPort getPopularProductsInputPort;
        @Mock
        private GetPersonalizedProductsInputPort getPersonalizedProductsInputPort;
        @Mock
        private TrackProductViewInputPort trackProductViewInputPort;
        @Mock
        private GetProductAnalyticsInputPort getProductAnalyticsInputPort;
        @Mock
        private SearchProductCatalogInputPort searchProductCatalogInputPort;

        private MockMvc mockMvc;
        private final JsonMapper objectMapper = JacksonMapperFactory.create();

        @BeforeEach
        void setUp() {
                ProductController controller = new ProductController(
                                createProductInputPort, cloneProductInputPort, defineVariantInputPort,
                                changeVariantPriceInputPort, changeVariantParcelInputPort, bulkPriceUpdateInputPort,
                                assignBrandInputPort,
                                assignCategoriesInputPort, defineAttributesInputPort, publishProductInputPort,
                                submitForReviewInputPort, rejectProductInputPort, deactivateProductInputPort,
                                restoreProductInputPort, getProductInputPort, getProductsBySkuIdsInputPort,
                                listProductsByMerchantInputPort, listProductsByStatusInputPort,
                                removeVariantInputPort, updateMediaInputPort, updateAiMetadataInputPort,
                                assignCollectionsInputPort, emergencyTakedownInputPort, searchProductsInputPort,
                                getRelatedProductsInputPort, getPopularProductsInputPort,
                                getPersonalizedProductsInputPort, trackProductViewInputPort,
                                getProductAnalyticsInputPort, searchProductCatalogInputPort,
                                new ProductSearchCriteriaAssembler(),
                                Mappers.getMapper(ProductDtoMapper.class));
                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new CatalogExceptionHandler())
                                .setCustomArgumentResolvers(
                                                new CurrentMerchantIdArgumentResolver(
                                                                new MockOwnershipVerifier()),
                                                new CurrentAdminIdArgumentResolver(),
                                                new CurrentOwnerIdArgumentResolver(),
                                                new OptionalOwnerIdArgumentResolver())
                                .addInterceptors(new MockSecurityInterceptor())
                                .setMessageConverters(new JacksonJsonHttpMessageConverter(objectMapper))
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
                                                "sku-1", Map.of("color", "red"), new BigDecimal("10.00"), "VND",
                                                500, BigDecimal.valueOf(15), BigDecimal.valueOf(10),
                                                BigDecimal.valueOf(5)))))
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
        void changeVariantParcelReturnsOk() throws Exception {
                when(changeVariantParcelInputPort.execute(any(ChangeVariantParcelCommand.class)))
                                .thenReturn(sample());

                mockMvc.perform(put("/api/v1/catalog/products/" + PRODUCT_ID + "/variants/sku-1/parcel")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new ChangeVariantParcelRequest(
                                                500, new BigDecimal("15"), new BigDecimal("10"),
                                                new BigDecimal("5")))))
                                .andExpect(status().isOk());
        }

        @Test
        void changeVariantParcelRejectsMissingWeight() throws Exception {
                mockMvc.perform(put("/api/v1/catalog/products/" + PRODUCT_ID + "/variants/sku-1/parcel")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"lengthCm\":15,\"widthCm\":10,\"heightCm\":5}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void bulkPriceUpdateReturnsOk() throws Exception {
                when(bulkPriceUpdateInputPort.execute(any(BulkPriceUpdateCommand.class)))
                                .thenReturn(new BulkPriceUpdateResult(1, 0, List.of()));

                mockMvc.perform(put("/api/v1/catalog/products/variants/prices")
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
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID)))
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
                                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN")))
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

                mockMvc.perform(get("/api/v1/catalog/products/by-skus").param("skuIds", "sku-1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].productId").value(PRODUCT_ID));
        }

        @Test
        void listByMerchantReturnsOk() throws Exception {
                when(listProductsByMerchantInputPort.execute(any(ListProductsByMerchantQuery.class)))
                                .thenReturn(new PageResult<>(List.of(sample()), 0, 20, 1));

                mockMvc.perform(get("/api/v1/catalog/products/merchant")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].productId").value(PRODUCT_ID));

                verify(listProductsByMerchantInputPort).execute(any(ListProductsByMerchantQuery.class));
        }

        @Test
        void listByStatusReturnsOk() throws Exception {
                when(listProductsByStatusInputPort.execute(any(ListProductsByStatusQuery.class)))
                                .thenReturn(new PageResult<>(List.of(sample()), 0, 20, 1));

                mockMvc.perform(get("/api/v1/catalog/products/admin/reviews")
                                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].productId").value(PRODUCT_ID));

                verify(listProductsByStatusInputPort).execute(any(ListProductsByStatusQuery.class));
        }

        @Test
        void removeVariantReturnsOk() throws Exception {
                when(removeVariantInputPort.execute(
                                 any(RemoveVariantCommand.class)))
                                .thenReturn(sample());

                mockMvc.perform(post("/api/v1/catalog/products/" + PRODUCT_ID + "/variants/sku-1/remove")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID)))
                                .andExpect(status().isOk());
        }

        @Test
        void updateMediaReturnsOk() throws Exception {
                when(updateMediaInputPort.execute(
                                any(UpdateMediaCommand.class)))
                                .thenReturn(sample());

                mockMvc.perform(put("/api/v1/catalog/products/" + PRODUCT_ID + "/media")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new UpdateMediaRequest(
                                                                List.of("img1", "img2")))))
                                .andExpect(status().isOk());
        }

        @Test
        void updateAiMetadataReturnsOk() throws Exception {
                when(updateAiMetadataInputPort.execute(
                                any(UpdateAiMetadataCommand.class)))
                                .thenReturn(sample());

                mockMvc.perform(put("/api/v1/catalog/products/" + PRODUCT_ID + "/ai-metadata")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new UpdateAiMetadataRequest(
                                                                List.of("premium"), "handmade"))))
                                .andExpect(status().isOk());
        }

        @Test
        void assignCollectionsReturnsOk() throws Exception {
                when(assignCollectionsInputPort.execute(
                                any(AssignCollectionsCommand.class)))
                                .thenReturn(sample());

                mockMvc.perform(put("/api/v1/catalog/products/" + PRODUCT_ID + "/collections")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new AssignCollectionsRequest(
                                                                List.of("coll-1")))))
                                .andExpect(status().isOk());
        }

        @Test
        void emergencyTakedownReturnsOk() throws Exception {
                when(emergencyTakedownInputPort.execute(
                                any(EmergencyTakedownCommand.class)))
                                .thenReturn(sample());

                mockMvc.perform(post("/api/v1/catalog/products/" + PRODUCT_ID + "/takedown")
                                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new EmergencyTakedownRequest(
                                                                "abuse"))))
                                .andExpect(status().isOk());
        }

        @Test
        void searchReturnsOk() throws Exception {
                when(searchProductsInputPort.execute(
                                any(SearchProductsQuery.class)))
                                .thenReturn(new PageResult<>(List.of(sample()), 0, 20, 1));

                mockMvc.perform(get("/api/v1/catalog/products/search").param("keyword", "widget"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].productId").value(PRODUCT_ID));
        }

        @Test
        void getRelatedProductsReturnsOk() throws Exception {
                when(getRelatedProductsInputPort.execute(
                                any(GetRelatedProductsQuery.class)))
                                .thenReturn(List.of(sample()));

                mockMvc.perform(get("/api/v1/catalog/products/" + PRODUCT_ID + "/recommendations").param("limit", "5"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].productId").value(PRODUCT_ID));
        }

        @Test
        void getPopularProductsReturnsOk() throws Exception {
                when(getPopularProductsInputPort.execute(
                                any(GetPopularProductsQuery.class)))
                                .thenReturn(List.of(sample()));

                mockMvc.perform(get("/api/v1/catalog/products/recommendations/popular"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].productId").value(PRODUCT_ID));
        }

        @Test
        void getPersonalizedProductsReturnsOk() throws Exception {
                when(getPersonalizedProductsInputPort.execute(
                                any(GetPersonalizedProductsQuery.class)))
                                .thenReturn(List.of(sample()));

                mockMvc.perform(get("/api/v1/catalog/products/recommendations/personalized")
                                .param("categoryIds", "c1").param("limit", "5"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].productId").value(PRODUCT_ID));
        }

        @Test
        void trackViewReturnsOk() throws Exception {
                mockMvc.perform(post("/api/v1/catalog/products/" + PRODUCT_ID + "/view")
                                .with(TestAuth.authMerchant("owner-1", MERCHANT_ID)))
                                .andExpect(status().isOk());

                verify(trackProductViewInputPort).execute(
                                any(TrackProductViewCommand.class));
        }

        @Test
        void adminAnalyticsReturnsOk() throws Exception {
                when(getProductAnalyticsInputPort.execute()).thenReturn(
                                new ProductAnalyticsResult(
                                                1, 2, 3, 4, 5, 4.5, List.of()));

                mockMvc.perform(get("/api/v1/catalog/products/admin/analytics")
                                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.totalPublished").value(1));
        }

        @Test
        void searchCatalogReturnsOkAndPassesTheAuthenticatedUserToTheUseCase() throws Exception {
                ProductSearchResult result = ProductSearchResult
                                .of(
                                                new PageResult<>(List.of(sample()), 0, 20, 1));
                when(searchProductCatalogInputPort.execute(
                                any(ProductSearchCriteria.class), any()))
                                .thenReturn(result);

                mockMvc.perform(get("/api/v1/catalog/products/search/catalog")
                                .with(TestAuth.authUser("user-1"))
                                .param("q", "widget")
                                .param("brandIds", "b1")
                                .param("attr.color", "red")
                                .param("sort", "PRICE_ASC"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.page.content[0].productId").value(PRODUCT_ID));

                verify(searchProductCatalogInputPort).execute(
                                any(ProductSearchCriteria.class), eq("user-1"));
        }

        @Test
        void searchCatalogPassesAnonymousCallersWithoutAUser() throws Exception {
                ProductSearchResult result = ProductSearchResult
                                .of(
                                                new PageResult<>(List.of(sample()), 0, 20, 1));
                when(searchProductCatalogInputPort.execute(
                                any(ProductSearchCriteria.class), any()))
                                .thenReturn(result);

                mockMvc.perform(get("/api/v1/catalog/products/search/catalog")
                                .param("q", "widget"))
                                .andExpect(status().isOk());

                verify(searchProductCatalogInputPort).execute(
                                any(ProductSearchCriteria.class), isNull());
        }
}
