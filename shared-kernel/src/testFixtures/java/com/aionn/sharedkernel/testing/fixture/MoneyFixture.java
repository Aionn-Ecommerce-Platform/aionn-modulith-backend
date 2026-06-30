package com.aionn.sharedkernel.testing.fixture;

import com.aionn.sharedkernel.domain.vo.Money;

import java.math.BigDecimal;

public final class MoneyFixture {

    private MoneyFixture() {
    }

    public static final Money ZERO_VND = Money.zero("VND");
    public static final Money SHIPPING_FEE_VND = Money.of(new BigDecimal("30000"), "VND");
    public static final Money LOW_VALUE_VND = Money.of(new BigDecimal("50000"), "VND");
    public static final Money MID_VALUE_VND = Money.of(new BigDecimal("500000"), "VND");
    public static final Money HIGH_VALUE_VND = Money.of(new BigDecimal("5000000"), "VND");
    public static final Money FREE_SHIPPING_THRESHOLD = Money.of(new BigDecimal("300000"), "VND");

    public static final Money ZERO_USD = Money.zero("USD");
    public static final Money ONE_USD = Money.of(new BigDecimal("1.00"), "USD");
    public static final Money TEN_USD = Money.of(new BigDecimal("10.00"), "USD");
    public static final Money HUNDRED_USD = Money.of(new BigDecimal("100.00"), "USD");

    public static Money vnd(long amount) {
        return Money.of(BigDecimal.valueOf(amount), "VND");
    }

    public static Money vnd(String amount) {
        return Money.of(new BigDecimal(amount), "VND");
    }

    public static Money usd(String amount) {
        return Money.of(new BigDecimal(amount), "USD");
    }

    public static Money usd(double amount) {
        return Money.of(BigDecimal.valueOf(amount), "USD");
    }
}
