package com.aionn.promotion.application.dto.campaign.command;

import com.aionn.promotion.domain.valueobject.CampaignType;
import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateCampaignCommand(
        String name,
        CampaignType type,
        BigDecimal budget,
        String currency,
        Instant startDate,
        Instant endDate,
        String createdBy) implements Command {
}
