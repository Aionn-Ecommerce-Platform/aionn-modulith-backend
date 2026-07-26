package com.aionn.promotion.application.usecase.campaign;

import com.aionn.promotion.application.dto.campaign.result.CampaignResult;
import com.aionn.promotion.application.mapper.CampaignResultMapper;
import com.aionn.promotion.application.port.in.campaign.ListCampaignsByStatusInputPort;
import com.aionn.promotion.application.service.PromotionCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListCampaignsByStatusUseCase implements ListCampaignsByStatusInputPort {

    private final PromotionCampaignService promotionCampaignService;
    private final CampaignResultMapper campaignResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CampaignResult> execute(String status, int limit) {
        return campaignResultMapper.toResults(promotionCampaignService.listByStatus(status, limit));
    }
}
