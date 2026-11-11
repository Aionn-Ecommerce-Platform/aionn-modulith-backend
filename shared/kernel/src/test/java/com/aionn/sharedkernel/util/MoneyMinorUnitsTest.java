package com.aionn.sharedkernel.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyMinorUnitsTest {

    @Test
    void treatsAMissingAmountAsZero() {
        assertThat(MoneyMinorUnits.toMinorUnits(null, "VND")).isZero();
    }

    @Test
    void scalesByTheCurrencyFractionDigits() {
        assertThat(MoneyMinorUnits.toMinorUnits(new BigDecimal("12.34"), "USD")).isEqualTo(1234L);
        assertThat(MoneyMinorUnits.toMinorUnits(new BigDecimal("1234"), "VND")).isEqualTo(1234L);
        assertThat(MoneyMinorUnits.toMinorUnits(new BigDecimal("1234"), "JPY")).isEqualTo(1234L);
    }

    @Test
    void roundsHalfUp() {
        assertThat(MoneyMinorUnits.toMinorUnits(new BigDecimal("12.345"), "USD")).isEqualTo(1235L);
        assertThat(MoneyMinorUnits.toMinorUnits(new BigDecimal("12.344"), "USD")).isEqualTo(1234L);
    }

    @Test
    void fallsBackToTwoDecimalsForUnknownOrBlankCurrencies() {
        assertThat(MoneyMinorUnits.toMinorUnits(new BigDecimal("1.23"), null)).isEqualTo(123L);
        assertThat(MoneyMinorUnits.toMinorUnits(new BigDecimal("1.23"), "  ")).isEqualTo(123L);
        assertThat(MoneyMinorUnits.toMinorUnits(new BigDecimal("1.23"), "NOT_A_CURRENCY")).isEqualTo(123L);
    }

    @Test
    void keepsTheSignOfNegativeAmounts() {
        assertThat(MoneyMinorUnits.toMinorUnits(new BigDecimal("-1.50"), "USD")).isEqualTo(-150L);
    }
}
