package com.aionn.catalog.adapter.rest.mapper.merchant;

import com.aionn.catalog.adapter.rest.dto.merchant.request.AdminReasonRequest;
import com.aionn.catalog.adapter.rest.dto.merchant.request.RegisterMerchantRequest;
import com.aionn.catalog.adapter.rest.dto.merchant.request.UpdateCommissionRateRequest;
import com.aionn.catalog.adapter.rest.dto.merchant.request.UpdateMerchantProfileRequest;
import com.aionn.catalog.adapter.rest.dto.merchant.response.MerchantResponse;
import com.aionn.catalog.application.dto.merchant.command.ActivateMerchantCommand;
import com.aionn.catalog.application.dto.merchant.command.CloseMerchantCommand;
import com.aionn.catalog.application.dto.merchant.command.RegisterMerchantCommand;
import com.aionn.catalog.application.dto.merchant.command.SuspendMerchantCommand;
import com.aionn.catalog.application.dto.merchant.command.UpdateGlobalCommissionRateCommand;
import com.aionn.catalog.application.dto.merchant.command.UpdateMerchantCommissionRateCommand;
import com.aionn.catalog.application.dto.merchant.command.UpdateMerchantProfileCommand;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MerchantDtoMapper {

    RegisterMerchantCommand toRegisterMerchantCommand(String ownerId, RegisterMerchantRequest request);

    UpdateMerchantProfileCommand toUpdateMerchantProfileCommand(String merchantId, String ownerId, UpdateMerchantProfileRequest request);

    SuspendMerchantCommand toSuspendMerchantCommand(String merchantId, String adminId, AdminReasonRequest request);

    ActivateMerchantCommand toActivateMerchantCommand(String merchantId, String adminId, AdminReasonRequest request);

    CloseMerchantCommand toCloseMerchantCommand(String merchantId, String ownerId, AdminReasonRequest request);

    @Mapping(target = "defaultCommissionRate", source = "request.commissionRate")
    UpdateGlobalCommissionRateCommand toUpdateGlobalCommissionRateCommand(UpdateCommissionRateRequest request);

    UpdateMerchantCommissionRateCommand toUpdateMerchantCommissionRateCommand(String merchantId, UpdateCommissionRateRequest request);

    MerchantResponse toResponse(MerchantResult result);

    List<MerchantResponse> toResponses(List<MerchantResult> results);
}
