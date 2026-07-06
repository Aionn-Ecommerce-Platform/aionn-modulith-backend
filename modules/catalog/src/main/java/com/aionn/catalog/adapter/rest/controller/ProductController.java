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
import com.aionn.catalog.application.port.in.product.GetProductInputPort;
import com.aionn.catalog.application.port.in.product.GetProductsBySkuIdsInputPort;
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
        private final ProductDtoMapper productDtoMapper;

        @PostMapping
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Create product")
        public ResponseEntity<ApiResponse<ProductResult>> create(
                        @CurrentMerchantId String merchantId,
                        @Valid @RequestBody CreateProductRequest request) {
                ProductResult result = createProductInputPort.execute(
                                productDtoMapper.toCreateProductCommand(merchantId, request));
                return ApiResponse.createdResponse("Product created", result);
        }

        @PostMapping("/{productId}/clone")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Clone product")
        public ResponseEntity<ApiResponse<ProductResult>> clone(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId) {
                ProductResult result = cloneProductInputPort.execute(new CloneProductCommand(productId, merchantId));
                return ApiResponse.createdResponse("Product cloned", result);
        }

        @PostMapping("/{productId}/variants")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Define variant (SKU)")
        public ResponseEntity<ApiResponse<ProductResult>> defineVariant(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId,
                        @Valid @RequestBody DefineVariantRequest request) {
                ProductResult result = defineVariantInputPort.execute(
                                productDtoMapper.toDefineVariantCommand(productId, merchantId, request));
                return ResponseEntity.ok(ApiResponse.success(result, "Variant defined"));
        }

        @PutMapping("/{productId}/variants/{skuId}/price")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Change variant price")
        public ResponseEntity<ApiResponse<ProductResult>> changeVariantPrice(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId,
                        @PathVariable String skuId,
                        @Valid @RequestBody ChangeVariantPriceRequest request) {
                ProductResult result = changeVariantPriceInputPort.execute(
                                productDtoMapper.toChangeVariantPriceCommand(productId, merchantId, skuId, request));
                return ResponseEntity.ok(ApiResponse.success(result, "Variant price changed"));
        }

        @PostMapping("/bulk-price")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Bulk price update", description = "Update variant prices for many products at once")
        public ResponseEntity<ApiResponse<BulkPriceUpdateResult>> bulkPriceUpdate(
                        @CurrentMerchantId String merchantId,
                        @Valid @RequestBody BulkPriceUpdateRequest request) {
                BulkPriceUpdateResult result = bulkPriceUpdateInputPort.execute(
                                productDtoMapper.toBulkPriceUpdateCommand(merchantId, request));
                return ResponseEntity.ok(ApiResponse.success(result, "Bulk price update completed"));
        }

        @PutMapping("/{productId}/brand")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Assign brand")
        public ResponseEntity<ApiResponse<ProductResult>> assignBrand(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId,
                        @Valid @RequestBody AssignBrandRequest request) {
                ProductResult result = assignBrandInputPort.execute(
                                productDtoMapper.toAssignBrandCommand(productId, merchantId, request));
                return ResponseEntity.ok(ApiResponse.success(result, "Brand assigned"));
        }

        @PutMapping("/{productId}/categories")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Assign categories")
        public ResponseEntity<ApiResponse<ProductResult>> assignCategories(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId,
                        @Valid @RequestBody AssignCategoriesRequest request) {
                ProductResult result = assignCategoriesInputPort.execute(
                                productDtoMapper.toAssignCategoriesCommand(productId, merchantId, request));
                return ResponseEntity.ok(ApiResponse.success(result, "Categories assigned"));
        }

        @PutMapping("/{productId}/attributes")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Define attributes")
        public ResponseEntity<ApiResponse<ProductResult>> defineAttributes(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId,
                        @Valid @RequestBody DefineAttributesRequest request) {
                ProductResult result = defineAttributesInputPort.execute(
                                productDtoMapper.toDefineAttributesCommand(productId, merchantId, request));
                return ResponseEntity.ok(ApiResponse.success(result, "Attributes defined"));
        }

        @PostMapping("/{productId}/submit-review")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Submit for review")
        public ResponseEntity<ApiResponse<ProductResult>> submitForReview(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId) {
                ProductResult result = submitForReviewInputPort
                                .execute(new SubmitForReviewCommand(productId, merchantId));
                return ResponseEntity.ok(ApiResponse.success(result, "Product submitted for review"));
        }

        @PostMapping("/{productId}/publish")
        @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
        @Operation(summary = "Publish product")
        public ResponseEntity<ApiResponse<ProductResult>> publish(
                        @CurrentAdminId String adminId,
                        @PathVariable String productId) {
                ProductResult result = publishProductInputPort.execute(new PublishProductCommand(productId, adminId));
                return ResponseEntity.ok(ApiResponse.success(result, "Product published"));
        }

        @PostMapping("/{productId}/reject")
        @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
        @Operation(summary = "Reject product")
        public ResponseEntity<ApiResponse<ProductResult>> reject(
                        @CurrentAdminId String adminId,
                        @PathVariable String productId,
                        @Valid @RequestBody RejectProductRequest request) {
                ProductResult result = rejectProductInputPort.execute(
                                productDtoMapper.toRejectProductCommand(productId, adminId, request));
                return ResponseEntity.ok(ApiResponse.success(result, "Product rejected"));
        }

        @PostMapping("/{productId}/deactivate")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Deactivate product")
        public ResponseEntity<ApiResponse<ProductResult>> deactivate(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId,
                        @Valid @RequestBody DeactivateProductRequest request) {
                ProductResult result = deactivateProductInputPort.execute(
                                productDtoMapper.toDeactivateProductCommand(productId, merchantId, request));
                return ResponseEntity.ok(ApiResponse.success(result, "Product deactivated"));
        }

        @PostMapping("/{productId}/restore")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Restore product")
        public ResponseEntity<ApiResponse<ProductResult>> restore(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId) {
                ProductResult result = restoreProductInputPort
                                .execute(new RestoreProductCommand(productId, merchantId));
                return ResponseEntity.ok(ApiResponse.success(result, "Product restored"));
        }

        @GetMapping("/{productId}")
        @Operation(summary = "Get product")
        public ResponseEntity<ApiResponse<ProductResult>> get(@PathVariable String productId) {
                return ResponseEntity.ok(ApiResponse.success(
                                getProductInputPort.execute(new GetProductQuery(productId)), "Product fetched"));
        }

        @GetMapping("/by-sku")
        @Operation(summary = "Get products by SKU ids")
        public ResponseEntity<ApiResponse<List<ProductResult>>> getBySkuIds(@RequestParam List<String> skuIds) {
                return ResponseEntity.ok(ApiResponse.success(
                                getProductsBySkuIdsInputPort.execute(new GetProductsBySkuIdsQuery(skuIds)),
                                "Products fetched"));
        }

        @GetMapping
        @Operation(summary = "List products by merchant or status")
        public ResponseEntity<ApiResponse<List<ProductResult>>> list(
                        @RequestParam(required = false) String merchantId,
                        @RequestParam(required = false) ProductStatus status,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                PageResult<ProductResult> result;
                if (merchantId != null) {
                        result = listProductsByMerchantInputPort.execute(
                                        new ListProductsByMerchantQuery(merchantId, OffsetPagination.of(page, size)));
                } else if (status != null) {
                        result = listProductsByStatusInputPort.execute(
                                        new ListProductsByStatusQuery(status, OffsetPagination.of(page, size)));
                } else {
                        result = new PageResult<>(List.of(), page, size, 0);
                }
                return ResponseEntity.ok(ApiResponse.successWithPaging(
                                result.content(),
                                PageMetadata.of(result.page(), result.size(), result.totalElements()),
                                "Products fetched"));
        }

        @org.springframework.web.bind.annotation.DeleteMapping("/{productId}/variants/{skuId}")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Remove variant")
        public ResponseEntity<ApiResponse<ProductResult>> removeVariant(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId,
                        @PathVariable String skuId) {
                return ResponseEntity.ok(ApiResponse.success(
                                removeVariantInputPort.execute(
                                                new com.aionn.catalog.application.dto.product.command.RemoveVariantCommand(
                                                                productId, merchantId, skuId)),
                                "Variant removed"));
        }

        @PutMapping("/{productId}/media")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Update product media (images)")
        public ResponseEntity<ApiResponse<ProductResult>> updateMedia(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId,
                        @Valid @RequestBody com.aionn.catalog.adapter.rest.dto.product.UpdateMediaRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                updateMediaInputPort.execute(
                                                productDtoMapper.toUpdateMediaCommand(productId, merchantId, request)),
                                "Media updated"));
        }

        @PutMapping("/{productId}/ai-metadata")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Update AI-generated tags and description")
        public ResponseEntity<ApiResponse<ProductResult>> updateAiMetadata(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId,
                        @Valid @RequestBody com.aionn.catalog.adapter.rest.dto.product.UpdateAiMetadataRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                updateAiMetadataInputPort.execute(
                                                productDtoMapper.toUpdateAiMetadataCommand(productId, merchantId,
                                                                request)),
                                "AI metadata updated"));
        }

        @PutMapping("/{productId}/collections")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Assign product to collections")
        public ResponseEntity<ApiResponse<ProductResult>> assignCollections(
                        @CurrentMerchantId String merchantId,
                        @PathVariable String productId,
                        @Valid @RequestBody com.aionn.catalog.adapter.rest.dto.product.AssignCollectionsRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                assignCollectionsInputPort.execute(
                                                productDtoMapper.toAssignCollectionsCommand(productId, merchantId,
                                                                request)),
                                "Collections assigned"));
        }

        @PostMapping("/{productId}/takedown")
        @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
        @Operation(summary = "Admin emergency takedown")
        public ResponseEntity<ApiResponse<ProductResult>> emergencyTakedown(
                        @CurrentAdminId String adminId,
                        @PathVariable String productId,
                        @Valid @RequestBody com.aionn.catalog.adapter.rest.dto.product.EmergencyTakedownRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                emergencyTakedownInputPort.execute(
                                                productDtoMapper.toEmergencyTakedownCommand(productId, adminId,
                                                                request)),
                                "Product taken down"));
        }

        @GetMapping("/search")
        @Operation(summary = "Search products by keyword")
        public ResponseEntity<ApiResponse<List<ProductResult>>> search(
                        @RequestParam(required = false, defaultValue = "") String keyword,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                PageResult<ProductResult> result = searchProductsInputPort.execute(
                                new com.aionn.catalog.application.dto.product.query.SearchProductsQuery(
                                                keyword, OffsetPagination.of(page, size)));
                return ResponseEntity.ok(ApiResponse.successWithPaging(
                                result.content(),
                                PageMetadata.of(result.page(), result.size(), result.totalElements()),
                                "Products fetched"));
        }
}
