package com.aionn.promotion.application.dto.campaign.command;

import com.aionn.sharedkernel.application.command.Command;

public record ActivateCampaignCommand(String campaignId) implements Command {
}
