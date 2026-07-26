package com.aionn.promotion.application.port.in.campaign;

import com.aionn.promotion.application.dto.campaign.command.CampaignCommands;
import com.aionn.promotion.application.dto.campaign.result.CampaignResult;

public interface ActivateCampaignInputPort {
    CampaignResult execute(CampaignCommands.ActivateCampaign command);
}
