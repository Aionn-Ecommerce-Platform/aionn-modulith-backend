package com.aionn.promotion.application.port.in.campaign;

import com.aionn.promotion.application.dto.campaign.result.CampaignResult;

public interface GetCampaignInputPort {
    CampaignResult execute(String campaignId);
}
