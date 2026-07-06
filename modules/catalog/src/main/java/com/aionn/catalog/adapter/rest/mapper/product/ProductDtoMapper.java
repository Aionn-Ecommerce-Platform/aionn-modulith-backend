package com.aionn.catalog.adapter.rest.mapper.product;

import com.aionn.catalog.adapter.rest.dto.product.AssignBrandRequest;
import com.aionn.catalog.adapter.rest.dto.product.AssignCategoriesRequest;
import com.aionn.catalog.adapter.rest.dto.product.BulkPriceUpdateRequest;
import com.aionn.catalog.adapter.rest.dto.product.ChangeVariantPriceRequest;
import com.aionn.catalog.adapter.rest.dto.product.CreateProductRequest;
import com.aionn.catalog.adapter.rest.dto.product.DeactivateProductRequest;
import com.aionn.catalog.adapter.rest.dto.product.DefineAttributesRequest;
import com.aionn.catalog.adapter.rest.dto.product.DefineVariantRequest;
import com.aionn.catalog.adapter.rest.dto.product.RejectProductRequest;
import com.aionn.catalog.application.dto.product.command.AssignBrandCommand;
import com.aionn.catalog.application.dto.product.command.AssignCategoriesCommand;
import com.aionn.catalog.application.dto.product.command.BulkPriceUpdateCommand;
import com.aionn.catalog.application.dto.product.command.ChangeVariantPriceCommand;
import com.aionn.catalog.application.dto.product.command.CreateProductCommand;
import com.aionn.catalog.application.dto.product.command.DeactivateProductCommand;
import com.aionn.catalog.application.dto.product.command.DefineAttributesCommand;
import com.aionn.catalog.application.dto.product.command.DefineVariantCommand;
import com.aionn.catalog.application.dto.product.command.RejectProductCommand;
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
                .map(i -> new BulkPriceUpdateCommand.Item(i.productId(), i.skuId(), i.newPrice(), i.currency()))
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
}
