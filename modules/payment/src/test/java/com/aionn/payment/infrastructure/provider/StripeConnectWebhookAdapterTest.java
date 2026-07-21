package com.aionn.payment.infrastructure.provider;

import com.aionn.payment.application.port.out.stripeconnect.StripeConnectWebhookPort;
import com.aionn.payment.infrastructure.provider.config.StripeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeConnectWebhookAdapterTest {

    @Mock
    private StripeProperties stripeProperties;

    private StripeConnectWebhookAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new StripeConnectWebhookAdapter(stripeProperties);
    }

    @Test
    void parseAndVerifyShouldReturnNullWhenSignatureOrSecretIsMissing() {
        StripeConnectWebhookPort.WebhookEvent result = adapter.parseAndVerify("{}", null);
        assertNull(result);
    }
}
