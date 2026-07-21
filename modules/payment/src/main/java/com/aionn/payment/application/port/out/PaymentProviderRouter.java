package com.aionn.payment.application.port.out;

import com.aionn.payment.domain.valueobject.PaymentGatewayKind;

/** Picks the {@link PaymentProviderClient} for a given gateway kind. */
public interface PaymentProviderRouter {
    PaymentProviderClient route(PaymentGatewayKind kind);
}
