package com.aionn.promotion.application.dto.campaign.command;

import com.aionn.sharedkernel.application.command.Command;

public record EndCampaignCommand(String campaignId) implements Command {
}
