package com.aionn.sharedkernel.domain.vo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void normalizesTheCurrencyAndScale() {
        Money money = Money.of(new BigDecimal("12.345"), " usd ");

        assertThat(money.currency()).isEqualTo("USD");
        assertThat(money.amount()).isEqualByComparingTo("12.35");
    }

    @Test
    void supportsCurrenciesWithoutFractionDigits() {
        assertThat(Money.of(new BigDecimal("1500.6"), "JPY").amount()).isEqualByComparingTo("1501");
    }

    @Test
    void rejectsInvalidAmountsAndCurrencies() {
        assertThatThrownBy(() -> Money.of(null, "USD")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Money.of(BigDecimal.ONE, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Money.of(BigDecimal.ONE, "US")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.of(BigDecimal.ONE, "XYZ"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addsAndSubtractsWithinTheSameCurrency() {
        Money ten = Money.of(BigDecimal.TEN, "USD");

        assertThat(ten.add(Money.of(BigDecimal.ONE, "USD")).amount()).isEqualByComparingTo("11");
        assertThat(ten.subtract(Money.of(BigDecimal.ONE, "USD")).amount()).isEqualByComparingTo("9");
        assertThatThrownBy(() -> ten.add(Money.of(BigDecimal.ONE, "VND")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ten.add(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ten.subtract(Money.of(BigDecimal.ONE, "VND")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void multipliesByQuantitiesAndFactors() {
        Money ten = Money.of(BigDecimal.TEN, "USD");

        assertThat(ten.multiply(3).amount()).isEqualByComparingTo("30");
        assertThat(ten.multiply(new BigDecimal("1.5")).amount()).isEqualByComparingTo("15");
        assertThatThrownBy(() -> ten.multiply(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ten.multiply((BigDecimal) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void dividesWithTheCurrencyScale() {
        Money ten = Money.of(BigDecimal.TEN, "USD");

        assertThat(ten.divide(3).amount()).isEqualByComparingTo("3.33");
        assertThat(ten.divide(new BigDecimal("4")).amount()).isEqualByComparingTo("2.50");
        assertThatThrownBy(() -> ten.divide(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ten.divide(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ten.divide((BigDecimal) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ten.divide(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ten.divide(new BigDecimal("-2")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void appliesPercentagesAndNeverGoesBelowZero() {
        Money ten = Money.of(BigDecimal.TEN, "USD");

        assertThat(ten.minusPercent(BigDecimal.TEN).amount()).isEqualByComparingTo("9.00");
        assertThat(ten.minusPercent(new BigDecimal("150")).amount()).isEqualByComparingTo("0.00");
        assertThatThrownBy(() -> ten.minusPercent(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void comparesAndSelectsExtremes() {
        Money ten = Money.of(BigDecimal.TEN, "USD");
        Money five = Money.of(BigDecimal.valueOf(5), "USD");

        assertThat(ten.min(five)).isEqualTo(five);
        assertThat(ten.max(five)).isEqualTo(ten);
        assertThat(ten.isGreaterThan(five)).isTrue();
        assertThat(ten.isGreaterOrEqual(ten)).isTrue();
        assertThat(five.isLessThan(ten)).isTrue();
        assertThat(five.isLessOrEqual(five)).isTrue();
        assertThatThrownBy(() -> ten.min(Money.of(BigDecimal.ONE, "VND")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reportsTheSign() {
        assertThat(Money.zero("USD").isZero()).isTrue();
        assertThat(Money.of(BigDecimal.ONE, "USD").isPositive()).isTrue();
        assertThat(Money.of(BigDecimal.ONE, "USD").negate().isNegative()).isTrue();
    }

    @Test
    void buildsFromLongsAndZero() {
        assertThat(Money.of(5L, "VND").amount()).isEqualByComparingTo("5");
        assertThat(Money.zero("VND").amount()).isEqualByComparingTo("0");
    }

    @Test
    void comparisonsTreatEqualAmountsConsistently() {
        Money ten = Money.of(BigDecimal.TEN, "USD");
        Money same = Money.of(new BigDecimal("10.00"), "USD");

        assertThat(ten.isGreaterThan(same)).isFalse();
        assertThat(ten.isGreaterOrEqual(same)).isTrue();
        assertThat(ten.isLessThan(same)).isFalse();
        assertThat(ten.isLessOrEqual(same)).isTrue();
        assertThat(ten.min(same)).isSameAs(ten);
        assertThat(ten.max(same)).isSameAs(ten);
    }

    @Test
    void dividesByAFactorUsingTheCurrencyScale() {
        assertThat(Money.of(BigDecimal.TEN, "JPY").divide(new BigDecimal("4")).amount())
                .isEqualByComparingTo("3");
        assertThat(Money.of(BigDecimal.TEN, "USD").divide(new BigDecimal("3")).amount())
                .isEqualByComparingTo("3.33");
    }
}
