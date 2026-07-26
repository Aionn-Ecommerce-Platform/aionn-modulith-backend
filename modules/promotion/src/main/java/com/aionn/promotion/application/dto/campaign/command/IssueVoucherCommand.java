package com.aionn.promotion.application.dto.campaign.command;

import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;
import java.time.Instant;

public record IssueVoucherCommand(
        String campaignId,
        String voucherCode,
        BigDecimal discountAmount,
        String currency,
        int usageLimit,
        Instant validFrom,
        Instant validUntil) implements Command {
}
