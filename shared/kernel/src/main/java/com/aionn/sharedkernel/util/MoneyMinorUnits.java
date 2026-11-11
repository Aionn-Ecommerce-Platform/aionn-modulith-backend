package com.aionn.sharedkernel.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;

public final class MoneyMinorUnits {

    private static final int DEFAULT_FRACTION_DIGITS = 2;

    private MoneyMinorUnits() {
    }

    public static long toMinorUnits(BigDecimal amount, String currency) {
        if (amount == null) {
            return 0L;
        }
        int fractionDigits = fractionDigits(currency);
        return amount.movePointRight(Math.max(fractionDigits, 0))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private static int fractionDigits(String currency) {
        if (currency == null || currency.isBlank()) {
            return DEFAULT_FRACTION_DIGITS;
        }
        try {
            return Currency.getInstance(currency.toUpperCase(Locale.ROOT)).getDefaultFractionDigits();
        } catch (IllegalArgumentException ex) {
            return DEFAULT_FRACTION_DIGITS;
        }
    }
}
