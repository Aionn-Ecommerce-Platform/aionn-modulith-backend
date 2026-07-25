package com.aionn.shipping.application.dto.rate.command;

import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;

public record UpdateRateCommand(
        String rateId,
        BigDecimal baseFee,
        String condition) implements Command {
}
