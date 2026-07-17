package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.dto.product.request.AssignBrandRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.AssignCategoriesRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.BulkPriceUpdateRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.ChangeVariantPriceRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.CreateProductRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.DeactivateProductRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.DefineAttributesRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.DefineVariantRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.RejectProductRequest;
import com.aionn.catalog.adapter.rest.dto.product.response.BulkPriceUpdateResponse;
import com.aionn.catalog.adapter.rest.dto.product.response.ProductAnalyticsResponse;
import com.aionn.catalog.adapter.rest.dto.product.response.ProductResponse;
import com.aionn.catalog.adapter.rest.dto.product.response.ProductSearchResponse;
import com.aionn.catalog.adapter.rest.mapper.product.ProductDtoMapper;
import com.aionn.catalog.adapter.rest.support.session.CurrentAdminId;
import com.aionn.catalog.adapter.rest.support.session.CurrentMerchantId;
import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.product.command.CloneProductCommand;
import com.aionn.catalog.application.dto.product.command.PublishProductCommand;
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
import com.aionn.catalog.application.dto.analytics.result.ProductAnalyticsResult;
import com.aionn.catalog.application.dto.product.command.TrackProductViewCommand;
import com.aionn.catalog.application.dto.search.ProductSearchCriteria;
import com.aionn.catalog.application.dto.search.ProductSearchResult;
import com.aionn.catalog.application.port.in.product.GetProductAnalyticsInputPort;
import com.aionn.catalog.application.port.in.product.SearchProductCatalogInputPort;
import com.aionn.catalog.application.dto.product.query.GetPersonalizedProductsQuery;
import com.aionn.catalog.application.dto.product.query.GetPopularProductsQuery;
import com.aionn.catalog.application.dto.product.query.GetRelatedProductsQuery;
import com.aionn.catalog.application.port.in.product.GetPersonalizedProductsInputPort;
import com.aionn.catalog.application.port.in.product.GetPopularProductsInputPort;
import com.aionn.catalog.application.port.in.product.GetProductInputPort;
import com.aionn.catalog.application.port.in.product.GetProductsBySkuIdsInputPort;
import com.aionn.catalog.application.port.in.product.GetRelatedProductsInputPort;
import com.aionn.catalog.application.port.in.product.TrackProductViewInputPort;
import com.aionn.catalog.adapter.rest.support.session.CurrentOwnerId;
import org.springframework.security.core.Authentication;
import com.aionn.catalog.application.port.in.product.ListProductsByMerchantInputPort;
import com.aionn.catalog.application.port.in.product.ListProductsByStatusInputPort;
import com.aionn.catalog.application.port.in.product.PublishProductInputPort;
import com.aionn.catalog.application.port.in.product.RejectProductInputPort;
import com.aionn.catalog.application.port.in.product.RestoreProductInputPort;
import com.aionn.catalog.application.port.in.product.SubmitForReviewInputPort;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.aionn.sharedkernel.adapter.web.response.PageMetadata;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog/products")
@RequiredArgsConstructor
@Tag(name = "Catalog - Product", description = "Product lifecycle, variants, pricing")
public class ProductController {

        private static final String MSG_PRODUCTS_FETCHED = "Products fetched";

        private final CreateProductInputPort createProductInputPort;
        private final CloneProductInputPort cloneProductInputPort;
        private final DefineVariantInputPort defineVariantInputPort;
        private final ChangeVariantPriceInputPort changeVariantPriceInputPort;
        private final BulkPriceUpdateInputPort bulkPriceUpdateInputPort;
        private final AssignBrandInputPort assignBrandInputPort;
        private final AssignCategoriesInputPort assignCategoriesInputPort;
        private final DefineAttributesInputPort defineAttributesInputPort;
        private final PublishProductInputPort publishProductInputPort;
        private final SubmitForReviewInputPort submitForReviewInputPort;
        private final RejectProductInputPort rejectProductInputPort;
        private final DeactivateProductInputPort deactivateProductInputPort;
        private final RestoreProductInputPort restoreProductInputPort;
        private final GetProductInputPort getProductInputPort;
        private final GetProductsBySkuIdsInputPort getProductsBySkuIdsInputPort;
        private final ListProductsByMerchantInputPort listProductsByMerchantInputPort;
        private final ListProductsByStatusInputPort listProductsByStatusInputPort;
        private final com.aionn.catalog.application.port.in.product.RemoveVariantInputPort removeVariantInputPort;
        private final com.aionn.catalog.application.port.in.product.UpdateMediaInputPort updateMediaInputPort;
        private final com.aionn.catalog.application.port.in.product.UpdateAiMetadataInputPort updateAiMetadataInputPort;
        private final com.aionn.catalog.application.port.in.product.AssignCollectionsInputPort assignCollectionsInputPort;
        private final com.aionn.catalog.application.port.in.product.EmergencyTakedownInputPort emergencyTakedownInputPort;
        private final com.aionn.catalog.application.port.in.product.SearchProductsInputPort searchProductsInputPort;
        private final GetRelatedProductsInputPort getRelatedProductsInputPort;
        private final GetPopularProductsInputPort getPopularProductsInputPort;
        private final GetPersonalizedProductsInputPort getPersonalizedProductsInputPort;
        private final TrackProductViewInputPort trackProductViewInputPort;
        private final GetProductAnalyticsInputPort getProductAnalyticsInputPort;
        private final SearchProductCatalogInputPort searchProductCatalogInputPort;
        private final ProductDtoMapper productDtoMapper;

        @PostMapping
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Create product")
        public ResponseEntity<ApiResponse<ProductResponse>> create(
                        @CurrentMerchantId String merchantId,
                        @Valid @RequestBody CreateProductRequest request) {
                ProductResult result = createProductInputPort.execute(
                                productDtoMapper.toCreateProductCommand(merchantId, request));
                return ApiResponse.createdResponse("Product created", productDtoMapper.toResponse(result));
        }

        @PostMapping("/{productId}/clone")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Clone product")
        public ResponseEntity<ApiResponse<ProductResponse>> clone(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId) {
                ProductResult result = cloneProductInputPort.execute(new CloneProductCommand(productId, merchantId));
                return ApiResponse.createdResponse("Product cloned", productDtoMapper.toResponse(result));
        }

        @PostMapping("/{productId}/variants")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Define variant (SKU)")
        public ResponseEntity<ApiResponse<ProductResponse>> defineVariant(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId,
                        @Valid @RequestBody DefineVariantRequest request) {
                ProductResult result = defineVariantInputPort.execute(
                                productDtoMapper.toDefineVariantCommand(productId, merchantId, request));
                return ResponseEntity.ok(ApiResponse.success(productDtoMapper.toResponse(result), "Variant defined"));
        }

        @PutMapping("/{productId}/variants/{skuId}/price")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Change variant price")
        public ResponseEntity<ApiResponse<ProductResponse>> changeVariantPrice(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId,
                        @PathVariable String skuId,
                        @Valid @RequestBody ChangeVariantPriceRequest request) {
                ProductResult result = changeVariantPriceInputPort.execute(
                                productDtoMapper.toChangeVariantPriceCommand(productId, merchantId, skuId, request));
                return ResponseEntity.ok(ApiResponse.success(productDtoMapper.toResponse(result), "Variant price updated"));
        }

        @PutMapping("/variants/prices")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Bulk update prices")
        public ResponseEntity<ApiResponse<BulkPriceUpdateResponse>> bulkUpdatePrices(
                        @CurrentMerchantId String merchantId,
                        @Valid @RequestBody BulkPriceUpdateRequest request) {
                BulkPriceUpdateResult result = bulkPriceUpdateInputPort.execute(
                                productDtoMapper.toBulkPriceUpdateCommand(merchantId, request));
                return ResponseEntity.ok(ApiResponse.success(productDtoMapper.toBulkUpdateResponse(result), "Bulk prices updated"));
        }

        @PutMapping("/{productId}/brand")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Assign brand")
        public ResponseEntity<ApiResponse<ProductResponse>> assignBrand(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId,
                        @Valid @RequestBody AssignBrandRequest request) {
                ProductResult result = assignBrandInputPort.execute(
                                productDtoMapper.toAssignBrandCommand(productId, merchantId, request));
                return ResponseEntity.ok(ApiResponse.success(productDtoMapper.toResponse(result), "Brand assigned"));
        }

        @PutMapping("/{productId}/categories")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Assign categories")
        public ResponseEntity<ApiResponse<ProductResponse>> assignCategories(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId,
                        @Valid @RequestBody AssignCategoriesRequest request) {
                ProductResult result = assignCategoriesInputPort.execute(
                                productDtoMapper.toAssignCategoriesCommand(productId, merchantId, request));
                return ResponseEntity.ok(ApiResponse.success(productDtoMapper.toResponse(result), "Categories assigned"));
        }

        @PutMapping("/{productId}/attributes")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Define attributes")
        public ResponseEntity<ApiResponse<ProductResponse>> defineAttributes(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId,
                        @Valid @RequestBody DefineAttributesRequest request) {
                ProductResult result = defineAttributesInputPort.execute(
                                productDtoMapper.toDefineAttributesCommand(productId, merchantId, request));
                return ResponseEntity.ok(ApiResponse.success(productDtoMapper.toResponse(result), "Attributes defined"));
        }

        @PostMapping("/{productId}/publish")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Publish product")
        public ResponseEntity<ApiResponse<ProductResponse>> publish(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId) {
                ProductResult result = publishProductInputPort.execute(new PublishProductCommand(productId, merchantId));
                return ResponseEntity.ok(ApiResponse.success(productDtoMapper.toResponse(result), "Product published"));
        }

        @PostMapping("/{productId}/submit-review")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Submit product for review")
        public ResponseEntity<ApiResponse<ProductResponse>> submitForReview(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId) {
                ProductResult result = submitForReviewInputPort.execute(new SubmitForReviewCommand(productId, merchantId));
                return ResponseEntity.ok(ApiResponse.success(productDtoMapper.toResponse(result), "Product submitted for review"));
        }

        @PostMapping("/{productId}/reject")
        @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
        @Operation(summary = "Reject product creation/update")
        public ResponseEntity<ApiResponse<ProductResponse>> reject(
                        @CurrentAdminId String adminId,
                        @PathVariable String productId,
                        @Valid @RequestBody RejectProductRequest request) {
                ProductResult result = rejectProductInputPort.execute(
                                productDtoMapper.toRejectProductCommand(productId, adminId, request));
                return ResponseEntity.ok(ApiResponse.success(productDtoMapper.toResponse(result), "Product review rejected"));
        }

        @PostMapping("/{productId}/deactivate")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Deactivate product")
        public ResponseEntity<ApiResponse<ProductResponse>> deactivate(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId,
                        @Valid @RequestBody DeactivateProductRequest request) {
                ProductResult result = deactivateProductInputPort.execute(
                                productDtoMapper.toDeactivateProductCommand(productId, merchantId, request));
                return ResponseEntity.ok(ApiResponse.success(productDtoMapper.toResponse(result), "Product deactivated"));
        }

        @PostMapping("/{productId}/restore")
        @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
        @Operation(summary = "Restore archived/deleted product")
        public ResponseEntity<ApiResponse<ProductResponse>> restore(
                        @CurrentAdminId String adminId,
                        @PathVariable String productId) {
                ProductResult result = restoreProductInputPort.execute(new RestoreProductCommand(productId, adminId));
                return ResponseEntity.ok(ApiResponse.success(productDtoMapper.toResponse(result), "Product restored"));
        }

        @GetMapping("/{productId}")
        @Operation(summary = "Get product")
        public ResponseEntity<ApiResponse<ProductResponse>> get(@PathVariable String productId) {
                return ResponseEntity.ok(ApiResponse.success(
                                productDtoMapper.toResponse(getProductInputPort.execute(new GetProductQuery(productId))), "Product fetched"));
        }

        @GetMapping("/merchant")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "List merchant products")
        public ResponseEntity<ApiResponse<List<ProductResponse>>> listMyProducts(
                        @CurrentMerchantId String merchantId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                PageResult<ProductResult> results = listProductsByMerchantInputPort.execute(
                                new ListProductsByMerchantQuery(merchantId, OffsetPagination.of(page, size)));
                return ResponseEntity.ok(ApiResponse.successWithPaging(
                                productDtoMapper.toResponses(results.content()),
                                PageMetadata.of(results.page(), results.size(), results.totalElements()),
                                MSG_PRODUCTS_FETCHED));
        }

        @GetMapping("/admin/reviews")
        @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
        @Operation(summary = "List products pending review")
        public ResponseEntity<ApiResponse<List<ProductResponse>>> listPendingReviews(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                PageResult<ProductResult> results = listProductsByStatusInputPort.execute(
                                new ListProductsByStatusQuery(ProductStatus.PENDING_REVIEW, OffsetPagination.of(page, size)));
                return ResponseEntity.ok(ApiResponse.successWithPaging(
                                productDtoMapper.toResponses(results.content()),
                                PageMetadata.of(results.page(), results.size(), results.totalElements()),
                                MSG_PRODUCTS_FETCHED));
        }

        @PostMapping("/{productId}/variants/{skuId}/remove")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Remove variant")
        public ResponseEntity<ApiResponse<ProductResponse>> removeVariant(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId,
                        @PathVariable String skuId) {
                ProductResult result = removeVariantInputPort.execute(
                                new com.aionn.catalog.application.dto.product.command.RemoveVariantCommand(productId, skuId, merchantId));
                return ResponseEntity.ok(ApiResponse.success(productDtoMapper.toResponse(result), "Variant removed"));
        }

        @PutMapping("/{productId}/media")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Update product media")
        public ResponseEntity<ApiResponse<ProductResponse>> updateMedia(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId,
                        @Valid @RequestBody com.aionn.catalog.adapter.rest.dto.product.request.UpdateMediaRequest request) {
                ProductResult result = updateMediaInputPort.execute(
                                productDtoMapper.toUpdateMediaCommand(productId, merchantId, request));
                return ResponseEntity.ok(ApiResponse.success(productDtoMapper.toResponse(result), "Media updated"));
        }

        @PutMapping("/{productId}/ai-metadata")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Update product AI metadata")
        public ResponseEntity<ApiResponse<ProductResponse>> updateAiMetadata(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId,
                        @Valid @RequestBody com.aionn.catalog.adapter.rest.dto.product.request.UpdateAiMetadataRequest request) {
                ProductResult result = updateAiMetadataInputPort.execute(
                                productDtoMapper.toUpdateAiMetadataCommand(productId, merchantId, request));
                return ResponseEntity.ok(ApiResponse.success(productDtoMapper.toResponse(result), "AI metadata updated"));
        }

        @PutMapping("/{productId}/collections")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Assign product collections")
        public ResponseEntity<ApiResponse<ProductResponse>> assignCollections(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId,
                        @Valid @RequestBody com.aionn.catalog.adapter.rest.dto.product.request.AssignCollectionsRequest request) {
                ProductResult result = assignCollectionsInputPort.execute(
                                productDtoMapper.toAssignCollectionsCommand(productId, merchantId, request));
                return ResponseEntity.ok(ApiResponse.success(productDtoMapper.toResponse(result), "Collections assigned"));
        }

        @PostMapping("/{productId}/takedown")
        @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
        @Operation(summary = "Emergency product takedown")
        public ResponseEntity<ApiResponse<ProductResponse>> emergencyTakedown(
                        @CurrentAdminId String adminId,
                        @PathVariable String productId,
                        @Valid @RequestBody com.aionn.catalog.adapter.rest.dto.product.request.EmergencyTakedownRequest request) {
                ProductResult result = emergencyTakedownInputPort.execute(
                                productDtoMapper.toEmergencyTakedownCommand(productId, adminId, request));
                return ResponseEntity.ok(ApiResponse.success(productDtoMapper.toResponse(result), "Emergency takedown executed"));
        }

        @GetMapping("/search")
        @Operation(summary = "Search products")
        public ResponseEntity<ApiResponse<List<ProductResponse>>> search(
                        @RequestParam(required = false, defaultValue = "") String keyword,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                PageResult<ProductResult> results = searchProductsInputPort.execute(
                                new com.aionn.catalog.application.dto.product.query.SearchProductsQuery(keyword, OffsetPagination.of(page, size)));
                return ResponseEntity.ok(ApiResponse.successWithPaging(
                                productDtoMapper.toResponses(results.content()),
                                PageMetadata.of(results.page(), results.size(), results.totalElements()),
                                MSG_PRODUCTS_FETCHED));
        }

        @GetMapping("/by-skus")
        @Operation(summary = "Get products by SKU IDs")
        public ResponseEntity<ApiResponse<List<ProductResponse>>> getBySkuIds(@RequestParam List<String> skuIds) {
                return ResponseEntity.ok(ApiResponse.success(
                                productDtoMapper.toResponses(getProductsBySkuIdsInputPort.execute(new GetProductsBySkuIdsQuery(skuIds))),
                                MSG_PRODUCTS_FETCHED));
        }

        @GetMapping("/{productId}/recommendations")
        @Operation(summary = "Get related products")
        public ResponseEntity<ApiResponse<List<ProductResponse>>> getRelated(
                        @PathVariable String productId,
                        @RequestParam(defaultValue = "5") int limit) {
                return ResponseEntity.ok(ApiResponse.success(
                                productDtoMapper.toResponses(getRelatedProductsInputPort.execute(new GetRelatedProductsQuery(productId, limit))),
                                MSG_PRODUCTS_FETCHED));
        }

        @GetMapping("/recommendations/popular")
        @Operation(summary = "Get popular products")
        public ResponseEntity<ApiResponse<List<ProductResponse>>> getPopular(@RequestParam(defaultValue = "5") int limit) {
                return ResponseEntity.ok(ApiResponse.success(
                                productDtoMapper.toResponses(getPopularProductsInputPort.execute(new GetPopularProductsQuery(limit))),
                                MSG_PRODUCTS_FETCHED));
        }

        @GetMapping("/recommendations/personalized")
        @Operation(summary = "Get personalized products")
        public ResponseEntity<ApiResponse<List<ProductResponse>>> getPersonalized(
                        Authentication authentication,
                        @RequestParam(required = false) List<String> categoryIds,
                        @RequestParam(required = false) List<String> brandIds,
                        @RequestParam(defaultValue = "5") int limit) {
                String userId = authentication != null ? authentication.getName() : null;
                return ResponseEntity.ok(ApiResponse.success(
                                productDtoMapper.toResponses(getPersonalizedProductsInputPort.execute(new GetPersonalizedProductsQuery(userId, categoryIds, brandIds, limit))),
                                MSG_PRODUCTS_FETCHED));
        }

        @PostMapping("/{productId}/view")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Track product view")
        public ResponseEntity<ApiResponse<Void>> trackView(
                        @CurrentOwnerId String ownerId,
                        @PathVariable String productId) {
                trackProductViewInputPort.execute(new TrackProductViewCommand(productId, ownerId));
                return ResponseEntity.ok(ApiResponse.success(null, "Product view tracked"));
        }

        @GetMapping("/admin/analytics")
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "Get merchant product analytics")
        public ResponseEntity<ApiResponse<ProductAnalyticsResponse>> getMerchantAnalytics() {
                ProductAnalyticsResult result = getProductAnalyticsInputPort.execute();
                return ResponseEntity.ok(ApiResponse.success(productDtoMapper.toAnalyticsResponse(result), "Analytics fetched"));
        }

        @GetMapping("/search/catalog")
        @Operation(summary = "Search product catalog with facets")
        public ResponseEntity<ApiResponse<ProductSearchResponse>> searchProductCatalog(
                        @RequestParam(required = false) String q,
                        @RequestParam(required = false) String merchantId,
                        @RequestParam(required = false) List<String> categoryIds,
                        @RequestParam(required = false) List<String> brandIds,
                        @RequestParam(required = false) java.math.BigDecimal priceMin,
                        @RequestParam(required = false) java.math.BigDecimal priceMax,
                        @RequestParam(required = false) Double ratingMin,
                        @RequestParam(required = false, defaultValue = "RELEVANCE") String sort,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(required = false) java.util.Map<String, String> allParams) {
                ProductSearchCriteria.Sort sortEnum;
                try {
                        sortEnum = ProductSearchCriteria.Sort.valueOf(sort.toUpperCase(java.util.Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                        sortEnum = ProductSearchCriteria.Sort.RELEVANCE;
                }
                java.util.Map<String, List<String>> attributes = new java.util.LinkedHashMap<>();
                if (allParams != null) {
                        for (java.util.Map.Entry<String, String> e : allParams.entrySet()) {
                                if (e.getKey().startsWith("attr.") && e.getValue() != null && !e.getValue().isBlank()) {
                                        attributes.put(e.getKey().substring(5),
                                                        java.util.Arrays.stream(e.getValue().split(","))
                                                                        .map(String::trim)
                                                                        .filter(s -> !s.isEmpty())
                                                                        .toList());
                                }
                        }
                }
                ProductSearchCriteria criteria = new ProductSearchCriteria(
                                q, merchantId, ProductStatus.PUBLISHED,
                                categoryIds == null ? List.of() : categoryIds,
                                brandIds == null ? List.of() : brandIds,
                                priceMin, priceMax, attributes, sortEnum, page, size, ratingMin);
                ProductSearchResult result = searchProductCatalogInputPort.execute(criteria);
                return ResponseEntity.ok(ApiResponse.success(productDtoMapper.toSearchResponse(result), MSG_PRODUCTS_FETCHED));
        }
}
