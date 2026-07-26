package com.aionn.promotion.application.usecase.campaign;

import com.aionn.promotion.application.dto.campaign.command.CampaignCommands;
import com.aionn.promotion.application.dto.campaign.result.CampaignResult;
import com.aionn.promotion.application.mapper.CampaignResultMapper;
import com.aionn.promotion.application.port.in.campaign.CreateCampaignInputPort;
import com.aionn.promotion.application.service.PromotionCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateCampaignUseCase implements CreateCampaignInputPort {

    private final PromotionCampaignService promotionCampaignService;
    private final CampaignResultMapper campaignResultMapper;

    @Override
    @Transactional
    public CampaignResult execute(CampaignCommands.CreateCampaign command) {
        return campaignResultMapper.toResult(promotionCampaignService.create(command));
    }
}
