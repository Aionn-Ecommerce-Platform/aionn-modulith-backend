package com.aionn.ucp.domain.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class UcpCurrencyUtilTest {

    @Test
    void toMinorUnitsReturnsZeroForNull() {
        assertThat(UcpCurrencyUtil.toMinorUnits(null, "USD")).isZero();
    }

    @Test
    void toMinorUnitsConvertsTwoDecimalCurrency() {
        assertThat(UcpCurrencyUtil.toMinorUnits(BigDecimal.valueOf(10.50), "USD")).isEqualTo(1050L);
        assertThat(UcpCurrencyUtil.toMinorUnits(BigDecimal.valueOf(10.50), "EUR")).isEqualTo(1050L);
    }

    @Test
    void toMinorUnitsConvertsZeroDecimalCurrencies() {
        assertThat(UcpCurrencyUtil.toMinorUnits(BigDecimal.valueOf(50000), "VND")).isEqualTo(50000L);
        assertThat(UcpCurrencyUtil.toMinorUnits(BigDecimal.valueOf(1200), "JPY")).isEqualTo(1200L);
    }

    @Test
    void toMinorUnitsFallsBackToTwoDecimalsForUnknownCurrency() {
        assertThat(UcpCurrencyUtil.toMinorUnits(BigDecimal.valueOf(15.25), "UNKNOWN")).isEqualTo(1525L);
        assertThat(UcpCurrencyUtil.toMinorUnits(BigDecimal.valueOf(15.25), null)).isEqualTo(1525L);
    }

    @Test
    void fromMinorUnitsReturnsNullForNull() {
        assertThat(UcpCurrencyUtil.fromMinorUnits(null, "USD")).isNull();
    }

    @Test
    void fromMinorUnitsConvertsTwoDecimalCurrency() {
        assertThat(UcpCurrencyUtil.fromMinorUnits(1050L, "USD")).isEqualByComparingTo("10.50");
    }

    @Test
    void fromMinorUnitsConvertsZeroDecimalCurrencies() {
        assertThat(UcpCurrencyUtil.fromMinorUnits(50000L, "VND")).isEqualByComparingTo("50000");
    }
}
