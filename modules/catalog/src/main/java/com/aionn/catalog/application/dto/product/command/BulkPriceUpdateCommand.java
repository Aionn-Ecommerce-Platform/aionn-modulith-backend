package com.aionn.catalog.application.dto.product.command;

import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;
import java.util.List;

public record BulkPriceUpdateCommand(
        String merchantId,
        List<Item> items) implements Command {

    public record Item(String productId, String skuId, BigDecimal newPrice, String currency) {
    }
}
