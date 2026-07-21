package com.aionn.payment.application.dto.payout.command;

import java.math.BigDecimal;

public record RequestPayoutCommand(
        String ownerId,
        BigDecimal amount,
        String currency,
        String bankName,
        String bankAccountNo,
        String bankAccountName,
        String note) {
}
