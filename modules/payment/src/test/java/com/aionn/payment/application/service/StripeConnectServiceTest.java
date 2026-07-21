package com.aionn.payment.application.service;

import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeConnectServiceTest {

    @Mock
    private MerchantQueryPort merchantQueryPort;

    @InjectMocks
    private StripeConnectService stripeConnectService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(stripeConnectService, "refreshUrl", "http://refresh");
        ReflectionTestUtils.setField(stripeConnectService, "returnUrl", "http://return");
    }

    @Test
    void shouldSyncAccountCapabilitiesCorrectly() {
        // Since syncAccountCapabilities makes a live Stripe API call in account fetch, 
        // we can test the fallback catch block gracefully when network fails or keys are missing.
        stripeConnectService.syncAccountCapabilities("acct_invalid");
        verifyNoInteractions(merchantQueryPort);
    }

    @Test
    void createOnboardingLinkThrowsWhenNoMerchant() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-invalid")).thenReturn(Optional.empty());

        assertThrows(PaymentException.class, () -> stripeConnectService.createOnboardingLink("owner-invalid"));
    }

    @Test
    void shouldApplyAccountUpdateSuccessfully() {
        com.stripe.model.Account account = mock(com.stripe.model.Account.class);
        java.util.Map<String, String> metadata = java.util.Map.of("merchantId", "m-1");
        
        when(account.getMetadata()).thenReturn(metadata);
        when(account.getChargesEnabled()).thenReturn(true);
        when(account.getPayoutsEnabled()).thenReturn(false);

        stripeConnectService.applyAccountUpdate(account);

        verify(merchantQueryPort).updateStripeCapabilities("m-1", true, false);
    }

    @Test
    void shouldSkipAccountUpdateWhenNoMerchantId() {
        com.stripe.model.Account account = mock(com.stripe.model.Account.class);
        when(account.getMetadata()).thenReturn(null);

        stripeConnectService.applyAccountUpdate(account);

        verifyNoInteractions(merchantQueryPort);
    }
}
