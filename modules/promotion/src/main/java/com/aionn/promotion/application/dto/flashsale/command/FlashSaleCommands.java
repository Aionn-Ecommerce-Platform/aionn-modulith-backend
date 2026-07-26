package com.aionn.promotion.application.dto.flashsale.command;

import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;

public final class FlashSaleCommands {

    private FlashSaleCommands() {
    }

    public record RegisterFlashSale(
            String campaignId,
            String ownerId,
            String productId,
            String skuId,
            BigDecimal salePrice,
            String currency,
            int saleStock) implements Command {
    }

    public record ApproveFlashSale(String registrationId, String adminId) implements Command {
    }

    public record RejectFlashSale(String registrationId, String adminId, String reason) implements Command {
    }

    public record CancelFlashSale(String registrationId, String ownerId) implements Command {
    }
}
