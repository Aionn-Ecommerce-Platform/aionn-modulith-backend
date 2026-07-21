package com.aionn.payment.infrastructure.provider;

import com.aionn.payment.application.port.out.PaymentProviderClient;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultPaymentProviderRouterTest {

    @Mock
    private PaymentProviderClient stripeClient;

    @Mock
    private PaymentProviderClient vnpayClient;

    private DefaultPaymentProviderRouter router;

    @BeforeEach
    void setUp() {
        when(stripeClient.kind()).thenReturn(PaymentGatewayKind.STRIPE);
        when(vnpayClient.kind()).thenReturn(PaymentGatewayKind.VNPAY);

        router = new DefaultPaymentProviderRouter(List.of(stripeClient, vnpayClient));
        router.buildIndex();
    }

    @Test
    void routeReturnsCorrectClientForKind() {
        PaymentProviderClient client = router.route(PaymentGatewayKind.STRIPE);

        assertEquals(stripeClient, client);
    }

    @Test
    void routeThrowsExceptionWhenProviderNotFound() {
        DefaultPaymentProviderRouter emptyRouter = new DefaultPaymentProviderRouter(List.of());
        emptyRouter.buildIndex();
        assertThrows(PaymentException.class, () -> emptyRouter.route(PaymentGatewayKind.STRIPE));
    }
}
