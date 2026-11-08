package com.aionn.shipping.application.dto.rate.command;

import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;

public record UpdateRateCommand(
        String rateId,
        BigDecimal baseFee,
        String condition,
        Integer baseWeightGram,
        Integer stepWeightGram,
        BigDecimal stepFee,
        BigDecimal maxFee,
        BigDecimal freeShippingThreshold) implements Command {

    public UpdateRateCommand(String rateId, BigDecimal baseFee, String condition) {
        this(rateId, baseFee, condition, null, null, null, null, null);
    }
}
