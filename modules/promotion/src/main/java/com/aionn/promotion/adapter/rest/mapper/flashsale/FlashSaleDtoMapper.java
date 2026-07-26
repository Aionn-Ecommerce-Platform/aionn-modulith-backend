package com.aionn.promotion.adapter.rest.mapper.flashsale;

import com.aionn.promotion.adapter.rest.dto.flashsale.RegisterFlashSaleRequest;
import com.aionn.promotion.adapter.rest.dto.flashsale.RejectFlashSaleRequest;
import com.aionn.promotion.adapter.rest.dto.flashsale.response.ActiveFlashSaleResponse;
import com.aionn.promotion.adapter.rest.dto.flashsale.response.FlashSaleRegistrationResponse;
import com.aionn.promotion.application.dto.flashsale.command.FlashSaleCommands;
import com.aionn.promotion.application.dto.flashsale.result.ActiveFlashSaleResult;
import com.aionn.promotion.application.dto.flashsale.result.FlashSaleRegistrationResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FlashSaleDtoMapper {

    FlashSaleRegistrationResponse toResponse(FlashSaleRegistrationResult result);

    List<FlashSaleRegistrationResponse> toResponses(List<FlashSaleRegistrationResult> results);

    ActiveFlashSaleResponse toActiveResponse(ActiveFlashSaleResult result);

    List<ActiveFlashSaleResponse> toActiveResponses(List<ActiveFlashSaleResult> results);

    default FlashSaleCommands.RegisterFlashSale toRegisterCommand(String ownerId,
            RegisterFlashSaleRequest request) {
        return new FlashSaleCommands.RegisterFlashSale(request.campaignId(), ownerId,
                request.productId(), request.skuId(), request.salePrice(),
                request.currency(), request.saleStock());
    }

    default FlashSaleCommands.ApproveFlashSale toApproveCommand(String registrationId, String adminId) {
        return new FlashSaleCommands.ApproveFlashSale(registrationId, adminId);
    }

    default FlashSaleCommands.RejectFlashSale toRejectCommand(String registrationId, String adminId,
            RejectFlashSaleRequest request) {
        return new FlashSaleCommands.RejectFlashSale(registrationId, adminId, request.reason());
    }

    default FlashSaleCommands.CancelFlashSale toCancelCommand(String registrationId, String ownerId) {
        return new FlashSaleCommands.CancelFlashSale(registrationId, ownerId);
    }
}
