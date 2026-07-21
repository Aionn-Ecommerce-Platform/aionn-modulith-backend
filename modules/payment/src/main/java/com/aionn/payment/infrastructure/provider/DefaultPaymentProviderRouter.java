package com.aionn.payment.infrastructure.provider;

import com.aionn.payment.application.port.out.PaymentProviderClient;
import com.aionn.payment.application.port.out.PaymentProviderRouter;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DefaultPaymentProviderRouter implements PaymentProviderRouter {

    private final List<PaymentProviderClient> clients;

    private final Map<PaymentGatewayKind, PaymentProviderClient> index = new EnumMap<>(PaymentGatewayKind.class);

    @PostConstruct
    void buildIndex() {
        for (PaymentProviderClient c : clients) {
            index.put(c.kind(), c);
        }
    }

    @Override
    public PaymentProviderClient route(PaymentGatewayKind kind) {
        PaymentProviderClient client = index.get(kind);
        if (client == null) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_GATEWAY_ERROR,
                    "No provider client wired for " + kind);
        }
        return client;
    }
}
