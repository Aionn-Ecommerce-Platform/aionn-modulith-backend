package com.aionn.catalog.adapter.rest.mapper.merchant;

import com.aionn.catalog.adapter.rest.dto.merchant.AdminReasonRequest;
import com.aionn.catalog.adapter.rest.dto.merchant.RegisterMerchantRequest;
import com.aionn.catalog.adapter.rest.dto.merchant.UpdateMerchantProfileRequest;
import com.aionn.catalog.application.dto.merchant.command.ActivateMerchantCommand;
import com.aionn.catalog.application.dto.merchant.command.CloseMerchantCommand;
import com.aionn.catalog.application.dto.merchant.command.RegisterMerchantCommand;
import com.aionn.catalog.application.dto.merchant.command.SuspendMerchantCommand;
import com.aionn.catalog.application.dto.merchant.command.UpdateMerchantProfileCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.aionn.catalog.application.dto.merchant.command.UpdateGlobalCommissionRateCommand;
import com.aionn.catalog.application.dto.merchant.command.UpdateMerchantCommissionRateCommand;
import com.aionn.catalog.adapter.rest.dto.merchant.UpdateCommissionRateRequest;

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
}
