package com.aionn.ucp.application.policy;

import java.time.Duration;

/** Serving limits that are deployment-configurable while the wire contract stays pinned. */
public interface UcpProtocolPolicy {

    Duration cartExpiry();

    Duration checkoutTtl();

    String storefrontBaseUrl();

    int catalogSearchDefaultLimit();

    int catalogSearchMaxLimit();

    String marketCurrency();
}
