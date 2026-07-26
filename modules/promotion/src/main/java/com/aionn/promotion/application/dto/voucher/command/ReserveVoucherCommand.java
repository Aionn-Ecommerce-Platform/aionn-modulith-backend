package com.aionn.promotion.application.dto.voucher.command;

import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReserveVoucherCommand(
        String userId,
        String voucherCode,
        String orderId,
        BigDecimal orderValue,
        String currency,
        List<String> orderCategoryIds,
        Instant expiresAt) implements Command {
}
