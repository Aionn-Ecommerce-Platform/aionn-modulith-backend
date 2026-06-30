package com.aionn.sharedkernel.domain.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, String currency) {

    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Currency iso = parseCurrency(currency);
        currency = iso.getCurrencyCode();
        int scale = iso.getDefaultFractionDigits();
        if (scale < 0) {
            scale = 2;
        }
        amount = amount.setScale(scale, ROUNDING);
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money of(long amount, String currency) {
        return of(BigDecimal.valueOf(amount), currency);
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        ensureSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        ensureSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must be >= 0");
        }
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
    }

    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor must not be null");
        return new Money(this.amount.multiply(factor), this.currency);
    }

    public Money divide(int divisor) {
        if (divisor <= 0) {
            throw new IllegalArgumentException("divisor must be positive, got " + divisor);
        }
        return new Money(this.amount.divide(BigDecimal.valueOf(divisor), ROUNDING), this.currency);
    }

    public Money divide(BigDecimal divisor) {
        Objects.requireNonNull(divisor, "divisor must not be null");
        if (divisor.signum() <= 0) {
            throw new IllegalArgumentException("divisor must be positive");
        }
        Currency iso = Currency.getInstance(this.currency);
        int scale = iso.getDefaultFractionDigits() < 0 ? 2 : iso.getDefaultFractionDigits();
        return new Money(this.amount.divide(divisor, scale, ROUNDING), this.currency);
    }

    public Money applyPercent(BigDecimal percent) {
        Objects.requireNonNull(percent, "percent must not be null");
        BigDecimal multiplier = BigDecimal.ONE.subtract(percent.movePointLeft(2));
        if (multiplier.signum() < 0) {
            multiplier = BigDecimal.ZERO;
        }
        return new Money(amount.multiply(multiplier), currency);
    }

    public Money min(Money other) {
        ensureSameCurrency(other);
        return amount.compareTo(other.amount) <= 0 ? this : other;
    }

    public Money max(Money other) {
        ensureSameCurrency(other);
        return amount.compareTo(other.amount) >= 0 ? this : other;
    }

    public boolean isGreaterThan(Money other) {
        ensureSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isGreaterOrEqual(Money other) {
        ensureSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isLessThan(Money other) {
        ensureSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isLessOrEqual(Money other) {
        ensureSameCurrency(other);
        return this.amount.compareTo(other.amount) <= 0;
    }

    public Money negate() {
        return new Money(this.amount.negate(), this.currency);
    }

    public boolean isPositive() {
        return this.amount.signum() > 0;
    }

    public boolean isZero() {
        return this.amount.signum() == 0;
    }

    public boolean isNegative() {
        return this.amount.signum() < 0;
    }

    private void ensureSameCurrency(Money other) {
        Objects.requireNonNull(other, "other Money must not be null");
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }

    private static Currency parseCurrency(String code) {
        String normalized = code.trim().toUpperCase();
        if (normalized.length() != 3) {
            throw new IllegalArgumentException(
                    "Currency must be ISO-4217 3-letter code, got: " + code);
        }
        try {
            return Currency.getInstance(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown ISO-4217 currency code: " + code, ex);
        }
    }
}
