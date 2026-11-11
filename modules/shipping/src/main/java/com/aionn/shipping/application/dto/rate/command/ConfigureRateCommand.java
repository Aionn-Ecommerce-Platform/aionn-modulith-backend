package com.aionn.shipping.application.dto.rate.command;

import com.aionn.sharedkernel.application.command.Command;

import java.math.BigDecimal;

public record ConfigureRateCommand(
        String zoneCode,
        BigDecimal baseFee,
        String currency,
        String condition,
        Integer baseWeightGram,
        Integer stepWeightGram,
        BigDecimal stepFee,
        BigDecimal maxFee,
        BigDecimal freeShippingThreshold,
        BigDecimal expressExtraFee,
        Integer expressEtaDays) implements Command {

    public ConfigureRateCommand(String zoneCode, BigDecimal baseFee, String currency, String condition) {
        this(zoneCode, baseFee, currency, condition, null, null, null, null, null, null, null);
    }
}
