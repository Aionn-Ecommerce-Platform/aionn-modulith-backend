package com.aionn.ordering.adapter.rest.mapper;

import com.aionn.ordering.adapter.rest.dto.request.*;
import com.aionn.ordering.adapter.rest.dto.response.CartResponse;
import com.aionn.ordering.adapter.rest.dto.response.OrderResponse;
import com.aionn.ordering.adapter.rest.dto.response.OrderReturnResponse;
import com.aionn.ordering.adapter.rest.dto.response.MerchantOrderAnalyticsResponse;
import com.aionn.ordering.adapter.rest.dto.response.PlatformOrderAnalyticsResponse;
import com.aionn.ordering.adapter.rest.dto.response.TopProductResponse;
import com.aionn.ordering.adapter.rest.dto.response.ReturnAnalyticsResponse;
import com.aionn.ordering.application.dto.cart.command.*;
import com.aionn.ordering.application.dto.cart.result.CartResult;
import com.aionn.ordering.application.dto.order.command.*;
import com.aionn.ordering.application.dto.order.result.OrderResult;
import com.aionn.ordering.application.dto.order.result.MerchantOrderAnalyticsResult;
import com.aionn.ordering.application.dto.order.result.PlatformOrderAnalyticsResult;
import com.aionn.ordering.application.dto.order.result.TopProductResult;
import com.aionn.ordering.application.dto.returns.command.*;
import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.application.dto.returns.result.ReturnAnalyticsResult;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderingDtoMapper {

    // Cart mappings - MapStruct auto-maps matching parameter names
    AddItemCommand toAddItemCommand(String userId, AddCartItemRequest request);

    UpdateItemQtyCommand toUpdateItemQtyCommand(String userId, String skuId, UpdateCartItemRequest request);

    default RemoveItemCommand toRemoveItemCommand(String userId, String skuId) {
        return new RemoveItemCommand(userId, skuId);
    }

    default ClearCartCommand toClearCartCommand(String userId, String reason) {
        return new ClearCartCommand(userId, reason);
    }

    ApplyVoucherCommand toApplyVoucherCommand(String userId, ApplyVoucherRequest request);

    default RemoveVoucherCommand toRemoveVoucherCommand(String userId) {
        return new RemoveVoucherCommand(userId);
    }

    // Order mappings
    PlaceOrderCommand toPlaceOrderCommand(String userId, PlaceOrderRequest request);

    default ConfirmPreparationCommand toConfirmPreparationCommand(String orderId, String ownerId) {
        return new ConfirmPreparationCommand(orderId, ownerId);
    }

    CancelOrderCommand toCancelOrderCommand(String orderId, String userId, CancelOrderRequest request);

    RejectOrderCommand toRejectOrderCommand(String orderId, String ownerId, RejectOrderRequest request);

    ChangeShippingInfoCommand toChangeShippingInfoCommand(String orderId, String userId,
            ChangeShippingInfoRequest request);

    ConfirmShippedCommand toConfirmShippedCommand(String orderId, ConfirmShippedRequest request);

    default ConfirmDeliveredCommand toConfirmDeliveredCommand(String orderId) {
        return new ConfirmDeliveredCommand(orderId);
    }

    // Order Return mappings
    RequestReturnCommand toRequestReturnCommand(String orderId, String userId, RequestReturnRequest request);

    ApproveReturnCommand toApproveReturnCommand(String returnId, String ownerId, ApproveReturnRequest request);

    RejectReturnCommand toRejectReturnCommand(String returnId, String ownerId, RejectReturnRequest request);

    ConfirmItemReceivedCommand toConfirmItemReceivedCommand(String returnId, String ownerId,
            ConfirmItemReceivedRequest request);

    // Response mappings
    CartResponse toResponse(CartResult result);

    OrderResponse toResponse(OrderResult result);

    OrderReturnResponse toResponse(ReturnResult result);

    MerchantOrderAnalyticsResponse toResponse(MerchantOrderAnalyticsResult result);

    PlatformOrderAnalyticsResponse toResponse(PlatformOrderAnalyticsResult result);

    TopProductResponse toResponse(TopProductResult result);

    List<TopProductResponse> toTopProductResponses(List<TopProductResult> results);

    ReturnAnalyticsResponse toResponse(ReturnAnalyticsResult result);
}
