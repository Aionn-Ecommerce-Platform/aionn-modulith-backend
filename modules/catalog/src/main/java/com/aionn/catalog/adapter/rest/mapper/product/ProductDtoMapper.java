package com.aionn.catalog.adapter.rest.mapper.product;

import com.aionn.catalog.adapter.rest.dto.product.request.AssignBrandRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.AssignCategoriesRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.AssignCollectionsRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.BulkPriceUpdateRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.ChangeVariantPriceRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.CreateProductRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.DeactivateProductRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.DefineAttributesRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.DefineVariantRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.EmergencyTakedownRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.RejectProductRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.UpdateAiMetadataRequest;
import com.aionn.catalog.adapter.rest.dto.product.request.UpdateMediaRequest;
import com.aionn.catalog.adapter.rest.dto.product.response.BulkPriceUpdateResponse;
import com.aionn.catalog.adapter.rest.dto.product.response.ProductAnalyticsResponse;
import com.aionn.catalog.adapter.rest.dto.product.response.ProductResponse;
import com.aionn.catalog.adapter.rest.dto.product.response.ProductSearchResponse;
import com.aionn.catalog.application.dto.analytics.result.ProductAnalyticsResult;
import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.product.command.AssignBrandCommand;
import com.aionn.catalog.application.dto.product.command.AssignCategoriesCommand;
import com.aionn.catalog.application.dto.product.command.AssignCollectionsCommand;
import com.aionn.catalog.application.dto.product.command.BulkPriceUpdateCommand;
import com.aionn.catalog.application.dto.product.command.ChangeVariantPriceCommand;
import com.aionn.catalog.application.dto.product.command.CloneProductCommand;
import com.aionn.catalog.application.dto.product.command.CreateProductCommand;
import com.aionn.catalog.application.dto.product.command.DeactivateProductCommand;
import com.aionn.catalog.application.dto.product.command.DefineAttributesCommand;
import com.aionn.catalog.application.dto.product.command.DefineVariantCommand;
import com.aionn.catalog.application.dto.product.command.EmergencyTakedownCommand;
import com.aionn.catalog.application.dto.product.command.PublishProductCommand;
import com.aionn.catalog.application.dto.product.command.RejectProductCommand;
import com.aionn.catalog.application.dto.product.command.RemoveVariantCommand;
import com.aionn.catalog.application.dto.product.command.RestoreProductCommand;
import com.aionn.catalog.application.dto.product.command.SubmitForReviewCommand;
import com.aionn.catalog.application.dto.product.command.TrackProductViewCommand;
import com.aionn.catalog.application.dto.product.command.UpdateAiMetadataCommand;
import com.aionn.catalog.application.dto.product.command.UpdateMediaCommand;
import com.aionn.catalog.application.dto.product.result.BulkPriceUpdateResult;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.dto.search.ProductSearchResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductDtoMapper {

        CreateProductCommand toCreateProductCommand(String merchantId, CreateProductRequest request);

        DefineVariantCommand toDefineVariantCommand(String productId, String merchantId, DefineVariantRequest request);

        ChangeVariantPriceCommand toChangeVariantPriceCommand(String productId, String merchantId, String skuId,
                        ChangeVariantPriceRequest request);

        default BulkPriceUpdateCommand toBulkPriceUpdateCommand(String merchantId, BulkPriceUpdateRequest request) {
                List<BulkPriceUpdateCommand.Item> items = request.items().stream()
                                .map(i -> new BulkPriceUpdateCommand.Item(i.productId(), i.skuId(), i.newPrice(),
                                                i.currency()))
                                .toList();
                return new BulkPriceUpdateCommand(merchantId, items);
        }

        AssignBrandCommand toAssignBrandCommand(String productId, String merchantId, AssignBrandRequest request);

        AssignCategoriesCommand toAssignCategoriesCommand(String productId, String merchantId,
                        AssignCategoriesRequest request);

        DefineAttributesCommand toDefineAttributesCommand(String productId, String merchantId,
                        DefineAttributesRequest request);

        RejectProductCommand toRejectProductCommand(String productId, String adminId, RejectProductRequest request);

        DeactivateProductCommand toDeactivateProductCommand(String productId, String merchantId,
                        DeactivateProductRequest request);

        UpdateMediaCommand toUpdateMediaCommand(String productId, String merchantId, UpdateMediaRequest request);

        UpdateAiMetadataCommand toUpdateAiMetadataCommand(String productId, String merchantId,
                        UpdateAiMetadataRequest request);

        AssignCollectionsCommand toAssignCollectionsCommand(String productId, String merchantId,
                        AssignCollectionsRequest request);

        EmergencyTakedownCommand toEmergencyTakedownCommand(String productId, String adminId,
                        EmergencyTakedownRequest request);

        default CloneProductCommand toCloneProductCommand(String productId, String merchantId) {
                return new CloneProductCommand(productId, merchantId);
        }

        default PublishProductCommand toPublishProductCommand(String productId, String merchantId) {
                return new PublishProductCommand(productId, merchantId);
        }

        default SubmitForReviewCommand toSubmitForReviewCommand(String productId, String merchantId) {
                return new SubmitForReviewCommand(productId, merchantId);
        }

        default RestoreProductCommand toRestoreProductCommand(String productId, String adminId) {
                return new RestoreProductCommand(productId, adminId);
        }

        default TrackProductViewCommand toTrackProductViewCommand(String productId, String ownerId) {
                return new TrackProductViewCommand(productId, ownerId);
        }

        default RemoveVariantCommand toRemoveVariantCommand(String productId, String skuId, String merchantId) {
                return new RemoveVariantCommand(productId, skuId, merchantId);
        }

        ProductResponse toResponse(ProductResult result);

        List<ProductResponse> toResponses(List<ProductResult> results);

        default PageResult<ProductResponse> toResponsePage(PageResult<ProductResult> page) {
                return new PageResult<>(toResponses(page.content()), page.page(), page.size(), page.totalElements());
        }

        ProductAnalyticsResponse toAnalyticsResponse(ProductAnalyticsResult result);

        BulkPriceUpdateResponse toBulkUpdateResponse(BulkPriceUpdateResult result);

        default ProductSearchResponse toSearchResponse(ProductSearchResult result) {
                if (result == null) {
                        return null;
                }
                ProductSearchResponse.Facets facets = null;
                if (result.facets() != null) {
                        ProductSearchResponse.PriceRange priceRange = null;
                        if (result.facets().priceRange() != null) {
                                priceRange = new ProductSearchResponse.PriceRange(result.facets().priceRange().min(),
                                                result.facets().priceRange().max());
                        }
                        facets = new ProductSearchResponse.Facets(
                                        result.facets().brands(),
                                        result.facets().categories(),
                                        result.facets().attributes(),
                                        priceRange);
                }
                return new ProductSearchResponse(toResponsePage(result.page()), facets);
        }
}
