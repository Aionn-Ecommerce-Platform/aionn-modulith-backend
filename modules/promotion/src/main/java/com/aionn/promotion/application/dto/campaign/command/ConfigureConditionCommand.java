package com.aionn.promotion.application.dto.campaign.command;

import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;
import java.util.List;

public record ConfigureConditionCommand(
        String campaignId,
        BigDecimal minOrderValue,
        List<String> applicableCategoryIds,
        Integer maxClaimsPerUser,
        Integer maxUsesPerVoucher) implements Command {
}
