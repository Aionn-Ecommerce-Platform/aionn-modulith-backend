package com.aionn.ucp.application.port.out;

/** Supplies the store currency used for UCP wire-facing minor-unit amounts. */
public interface UcpMarketCurrencyPort {
    String marketCurrency();
}
