package com.aionn.promotion.adapter.rest.mapper.campaign;

import com.aionn.promotion.adapter.rest.dto.campaign.CancelCampaignRequest;
import com.aionn.promotion.adapter.rest.dto.campaign.ConfigureConditionRequest;
import com.aionn.promotion.adapter.rest.dto.campaign.CreateCampaignRequest;
import com.aionn.promotion.adapter.rest.dto.campaign.response.CampaignResponse;
import com.aionn.promotion.adapter.rest.dto.voucher.IssueVoucherRequest;
import com.aionn.promotion.application.dto.campaign.command.CampaignCommands;
import com.aionn.promotion.application.dto.campaign.result.CampaignResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PromotionCampaignDtoMapper {

    CampaignResponse toResponse(CampaignResult result);

    List<CampaignResponse> toResponses(List<CampaignResult> results);

    default CampaignCommands.CreateCampaign toCreateCommand(String adminId, CreateCampaignRequest request) {
        return new CampaignCommands.CreateCampaign(request.name(), request.type(), request.budget(),
                request.currency(), request.startDate(), request.endDate(), adminId);
    }

    default CampaignCommands.ActivateCampaign toActivateCommand(String campaignId) {
        return new CampaignCommands.ActivateCampaign(campaignId);
    }

    default CampaignCommands.EndCampaign toEndCommand(String campaignId) {
        return new CampaignCommands.EndCampaign(campaignId);
    }

    default CampaignCommands.CancelCampaign toCancelCommand(String campaignId, CancelCampaignRequest request) {
        return new CampaignCommands.CancelCampaign(campaignId, request.reason());
    }

    default CampaignCommands.ConfigureCondition toConfigureConditionCommand(String campaignId,
            ConfigureConditionRequest request) {
        return new CampaignCommands.ConfigureCondition(campaignId, request.minOrderValue(),
                request.applicableCategoryIds(), request.maxClaimsPerUser(), request.maxUsesPerVoucher());
    }

    default CampaignCommands.IssueVoucher toIssueVoucherCommand(String campaignId, IssueVoucherRequest request) {
        return new CampaignCommands.IssueVoucher(campaignId, request.voucherCode(), request.discountAmount(),
                request.currency(), request.usageLimit(), request.validFrom(), request.validUntil());
    }
}
