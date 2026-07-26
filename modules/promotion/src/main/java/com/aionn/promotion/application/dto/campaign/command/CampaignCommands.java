package com.aionn.promotion.application.dto.campaign.command;

import com.aionn.promotion.domain.valueobject.CampaignType;
import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class CampaignCommands {

        private CampaignCommands() {
        }

        public record CreateCampaign(
                        String name,
                        CampaignType type,
                        BigDecimal budget,
                        String currency,
                        Instant startDate,
                        Instant endDate,
                        String createdBy) implements Command {
        }

        public record ActivateCampaign(String campaignId) implements Command {
        }

        public record EndCampaign(String campaignId) implements Command {
        }

        public record CancelCampaign(String campaignId, String reason) implements Command {
        }

        public record ConfigureCondition(
                        String campaignId,
                        BigDecimal minOrderValue,
                        List<String> applicableCategoryIds,
                        Integer maxClaimsPerUser,
                        Integer maxUsesPerVoucher) implements Command {
        }

        public record IssueVoucher(
                        String campaignId,
                        String voucherCode,
                        BigDecimal discountAmount,
                        String currency,
                        int usageLimit,
                        Instant validFrom,
                        Instant validUntil) implements Command {
        }
}
