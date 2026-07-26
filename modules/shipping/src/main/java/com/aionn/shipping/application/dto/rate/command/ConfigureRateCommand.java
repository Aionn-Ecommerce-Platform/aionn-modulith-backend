package com.aionn.shipping.application.dto.rate.command;

import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;

public record ConfigureRateCommand(
        String zoneCode,
        BigDecimal baseFee,
        String currency,
        String condition) implements Command {
}
