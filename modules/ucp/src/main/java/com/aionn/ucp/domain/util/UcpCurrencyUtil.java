package com.aionn.ucp.domain.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;

public final class UcpCurrencyUtil {

    private UcpCurrencyUtil() {
    }

    public static long toMinorUnits(BigDecimal price, String currency) {
        if (price == null) {
            return 0L;
        }
        int fractionDigits = 2;
        if (currency != null && !currency.isBlank()) {
            try {
                fractionDigits = Currency.getInstance(currency.toUpperCase(Locale.ROOT)).getDefaultFractionDigits();
            } catch (IllegalArgumentException ignored) {
                fractionDigits = 2;
            }
        }
        return price.movePointRight(Math.max(fractionDigits, 0))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }
}
