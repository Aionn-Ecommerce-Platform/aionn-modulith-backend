package com.aionn.sharedkernel.domain.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class MoneyPropertyTest {

    private static final String[] CURRENCIES = {
            "USD", "EUR", "VND", "JPY", "GBP", "AUD", "CAD", "CHF", "CNY", "KRW"
    };

    @Provide
    Arbitrary<BigDecimal> amounts() {
        Arbitrary<Long> integerPart = Arbitraries.longs().between(-999_999_999L, 999_999_999L);
        Arbitrary<Integer> scale = Arbitraries.integers().between(0, 10);
        return Combinators.combine(integerPart, scale).flatAs((ip, s) -> {
            if (s == 0) {
                return Arbitraries.just(new BigDecimal(ip));
            }
            return Arbitraries.strings().withCharRange('0', '9').ofLength(s)
                    .map(frac -> new BigDecimal((ip < 0 ? "-" : "") + Math.abs(ip) + "." + frac));
        });
    }

    @Provide
    Arbitrary<String> currencies() {
        return Arbitraries.of(CURRENCIES);
    }

    @Provide
    Arbitrary<String> invalidCurrencyStrings() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(0)
                .ofMaxLength(6)
                .filter(s -> !isValidIso(s.trim().toUpperCase()));
    }

    @Provide
    Arbitrary<Integer> negativeQuantities() {
        return Arbitraries.integers().lessOrEqual(-1);
    }

    @Provide
    Arbitrary<Integer> nonPositiveDivisors() {
        return Arbitraries.integers().lessOrEqual(0);
    }

    @Property(tries = 1000)
    void property1_roundsHalfUpToCurrencyScale(
            @ForAll("amounts") BigDecimal amount, @ForAll("currencies") String currency) {
        int digits = fractionDigits(currency);
        BigDecimal expected = amount.setScale(digits, RoundingMode.HALF_UP);

        Money money = new Money(amount, currency);

        assertEquals(expected, money.amount());
        assertEquals(digits, money.amount().scale());
    }

    @Property(tries = 1000)
    void property2_addThenSubtractIsInverse(
            @ForAll("amounts") BigDecimal amountA,
            @ForAll("amounts") BigDecimal amountB,
            @ForAll("currencies") String currency) {
        Money a = new Money(amountA, currency);
        Money b = new Money(amountB, currency);

        assertEquals(a, a.add(b).subtract(b));
    }

    @Property(tries = 1000)
    void property3_differentCurrencyOperationsThrow(
            @ForAll("amounts") BigDecimal amountA,
            @ForAll("amounts") BigDecimal amountB,
            @ForAll("currencies") String currencyA,
            @ForAll("currencies") String currencyB) {
        Assume.that(!currencyA.equals(currencyB));

        Money a = new Money(amountA, currencyA);
        Money b = new Money(amountB, currencyB);

        assertThrows(IllegalArgumentException.class, () -> a.add(b));
        assertThrows(IllegalArgumentException.class, () -> a.subtract(b));
        assertThrows(IllegalArgumentException.class, () -> a.min(b));
        assertThrows(IllegalArgumentException.class, () -> a.max(b));
        assertThrows(IllegalArgumentException.class, () -> a.isGreaterThan(b));
        assertThrows(IllegalArgumentException.class, () -> a.isGreaterOrEqual(b));
        assertThrows(IllegalArgumentException.class, () -> a.isLessThan(b));
        assertThrows(IllegalArgumentException.class, () -> a.isLessOrEqual(b));
    }

    @Property(tries = 1000)
    void property4_invalidCurrencyThrowsIae(
            @ForAll("amounts") BigDecimal amount,
            @ForAll("invalidCurrencyStrings") String invalidCurrency) {
        assertThrows(IllegalArgumentException.class, () -> new Money(amount, invalidCurrency));
    }

    @Property(tries = 1000)
    void property4_multiplyNegativeQuantityThrowsIae(
            @ForAll("amounts") BigDecimal amount,
            @ForAll("currencies") String currency,
            @ForAll("negativeQuantities") int quantity) {
        Money money = new Money(amount, currency);

        assertThrows(IllegalArgumentException.class, () -> money.multiply(quantity));
    }

    @Property(tries = 1000)
    void property4_divideNonPositiveDivisorThrowsIae(
            @ForAll("amounts") BigDecimal amount,
            @ForAll("currencies") String currency,
            @ForAll("nonPositiveDivisors") int divisor) {
        Money money = new Money(amount, currency);

        assertThrows(IllegalArgumentException.class, () -> money.divide(divisor));
    }

    @Property(tries = 1000)
    void property4_nullArgumentsThrowNpe(
            @ForAll("amounts") BigDecimal amount, @ForAll("currencies") String currency) {
        assertThrows(NullPointerException.class, () -> new Money(null, currency));
        assertThrows(NullPointerException.class, () -> new Money(amount, null));
        assertTrue(new Money(amount, currency).currency().length() == 3);
    }

    @net.jqwik.api.Example
    void example_moneyHelpersAndComparisonsCoverDeterministicBranches() {
        Money tenUsd = Money.of(new BigDecimal("10.00"), "usd");
        Money threeUsd = Money.of(3L, "USD");
        Money zeroUsd = Money.zero("USD");

        assertEquals(new BigDecimal("10.00"), tenUsd.amount());
        assertEquals(new BigDecimal("15.00"), tenUsd.multiply(new BigDecimal("1.5")).amount());
        assertEquals(new BigDecimal("5.00"), tenUsd.divide(2).amount());
        assertEquals(new BigDecimal("2.50"), tenUsd.divide(new BigDecimal("4")).amount());
        assertEquals(new BigDecimal("8.00"), tenUsd.applyPercent(new BigDecimal("20")).amount());
        assertEquals(BigDecimal.ZERO.setScale(2), tenUsd.applyPercent(new BigDecimal("120")).amount());

        assertEquals(threeUsd, tenUsd.min(threeUsd));
        assertEquals(tenUsd, tenUsd.max(threeUsd));
        assertTrue(tenUsd.isGreaterThan(threeUsd));
        assertTrue(tenUsd.isGreaterOrEqual(threeUsd));
        assertFalse(threeUsd.isGreaterThan(tenUsd));
        assertTrue(threeUsd.isLessThan(tenUsd));
        assertTrue(threeUsd.isLessOrEqual(tenUsd));
        assertEquals(new BigDecimal("-10.00"), tenUsd.negate().amount());
        assertTrue(tenUsd.isPositive());
        assertTrue(zeroUsd.isZero());
        assertTrue(tenUsd.negate().isNegative());
        assertNotEquals(tenUsd, tenUsd.negate());
    }

    @net.jqwik.api.Example
    void example_moneyRejectsNullOrInvalidOperandsInImperativeMethods() {
        Money tenUsd = Money.of(new BigDecimal("10.00"), "USD");

        assertThrows(NullPointerException.class, () -> tenUsd.multiply((BigDecimal) null));
        assertThrows(NullPointerException.class, () -> tenUsd.divide((BigDecimal) null));
        assertThrows(IllegalArgumentException.class, () -> tenUsd.divide(BigDecimal.ZERO));
        assertThrows(NullPointerException.class, () -> tenUsd.add(null));
        assertThrows(NullPointerException.class, () -> tenUsd.min(null));
    }

    private static int fractionDigits(String currency) {
        int digits = Currency.getInstance(currency).getDefaultFractionDigits();
        return digits < 0 ? 2 : digits;
    }

    private static boolean isValidIso(String normalized) {
        if (normalized.length() != 3) {
            return false;
        }
        try {
            Currency.getInstance(normalized);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
