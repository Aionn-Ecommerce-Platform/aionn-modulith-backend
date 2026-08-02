package com.aionn.payment.application.service;

import com.aionn.payment.application.port.out.StripeConnectPort;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeConnectServiceTest {

    @Mock
    private MerchantQueryPort merchantQueryPort;
    @Mock
    private StripeConnectPort stripeConnectPort;

    @InjectMocks
    private StripeConnectService stripeConnectService;

    @Test
    void shouldSyncAccountCapabilitiesCorrectly() {
        when(stripeConnectPort.fetchAccountCapabilities("acct_invalid"))
                .thenThrow(new PaymentException(PaymentErrorCode.PAYMENT_GATEWAY_ERROR, "failed"));
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
        var account = new StripeConnectPort.AccountCapabilities("acct_1", "m-1", true, false);

        stripeConnectService.applyAccountUpdate(account);

        verify(merchantQueryPort).updateStripeCapabilities("m-1", true, false);
    }

    @Test
    void shouldSkipAccountUpdateWhenNoMerchantId() {
        var account = new StripeConnectPort.AccountCapabilities("acct_1", null, false, false);

        stripeConnectService.applyAccountUpdate(account);

        verifyNoInteractions(merchantQueryPort);
    }

    @Test
    void createOnboardingLinkUsesExistingStripeAccount() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-1")).thenReturn(java.util.Optional.of("m-1"));

        MerchantQueryPort.StripeConnectInfo info = mock(MerchantQueryPort.StripeConnectInfo.class);
        when(info.stripeAccountId()).thenReturn("acct_existing");
        when(merchantQueryPort.findStripeConnectInfo("m-1")).thenReturn(java.util.Optional.of(info));

        when(stripeConnectPort.createOnboardingLink("acct_existing")).thenReturn("https://stripe/onboard");

        assertEquals("https://stripe/onboard", stripeConnectService.createOnboardingLink("owner-1"));
        verify(stripeConnectPort, never()).createExpressAccount(anyString());
    }

    @Test
    void createOnboardingLinkCreatesAndSavesMissingStripeAccount() {
        when(merchantQueryPort.findMerchantIdByOwnerId("owner-2")).thenReturn(java.util.Optional.of("m-2"));
        when(merchantQueryPort.findStripeConnectInfo("m-2")).thenReturn(java.util.Optional.empty());

        when(stripeConnectPort.createExpressAccount("m-2")).thenReturn("acct_new");
        when(stripeConnectPort.createOnboardingLink("acct_new")).thenReturn("https://stripe/onboard");

        assertEquals("https://stripe/onboard", stripeConnectService.createOnboardingLink("owner-2"));
        verify(merchantQueryPort).saveStripeAccountId("m-2", "acct_new");
    }
}
