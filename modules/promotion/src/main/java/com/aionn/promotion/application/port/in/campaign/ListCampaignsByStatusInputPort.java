package com.aionn.promotion.application.port.in.campaign;

import com.aionn.promotion.application.dto.campaign.result.CampaignResult;

import java.util.List;

public interface ListCampaignsByStatusInputPort {
    List<CampaignResult> execute(String status, int limit);
}
