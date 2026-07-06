package com.aionn.catalog.application.dto.product.command;

import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;
import java.util.Map;

public record DefineVariantCommand(
        String productId,
        String merchantId,
        String skuId,
        Map<String, String> attributeValues,
        BigDecimal price,
        String currency) implements Command {
}
