package com.aionn.promotion.application.usecase.campaign;

import com.aionn.promotion.application.dto.campaign.result.CampaignResult;
import com.aionn.promotion.application.mapper.CampaignResultMapper;
import com.aionn.promotion.application.port.in.campaign.GetCampaignInputPort;
import com.aionn.promotion.application.service.PromotionCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCampaignUseCase implements GetCampaignInputPort {

    private final PromotionCampaignService promotionCampaignService;
    private final CampaignResultMapper campaignResultMapper;

    @Override
    @Transactional(readOnly = true)
    public CampaignResult execute(String campaignId) {
        return campaignResultMapper.toResult(promotionCampaignService.get(campaignId));
    }
}
