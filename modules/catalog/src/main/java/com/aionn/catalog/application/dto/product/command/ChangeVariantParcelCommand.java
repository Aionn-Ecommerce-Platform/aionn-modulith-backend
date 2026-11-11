package com.aionn.catalog.application.dto.product.command;

import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;

public record ChangeVariantParcelCommand(
        String productId,
        String merchantId,
        String skuId,
        Integer weightGram,
        BigDecimal lengthCm,
        BigDecimal widthCm,
        BigDecimal heightCm) implements Command {
}
