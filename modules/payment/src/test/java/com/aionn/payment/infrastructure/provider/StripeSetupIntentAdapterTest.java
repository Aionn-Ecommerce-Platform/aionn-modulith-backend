package com.aionn.payment.infrastructure.provider;

import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.infrastructure.provider.config.StripeProperties;
import com.stripe.model.SetupIntent;
import com.stripe.exception.StripeException;
import com.stripe.net.RequestOptions;
import com.stripe.param.SetupIntentCreateParams;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StripeSetupIntentAdapterTest {

    private final StripeSetupIntentAdapter adapter =
            new StripeSetupIntentAdapter(new StripeProperties(true, "sk_test", "whsec_test"));

    @Test
    void createReturnsSetupIntentDetails() {
        try (MockedStatic<SetupIntent> setupIntents = Mockito.mockStatic(SetupIntent.class)) {
            SetupIntent intent = mock(SetupIntent.class);
            when(intent.getId()).thenReturn("si_1");
            when(intent.getClientSecret()).thenReturn("secret");
            setupIntents.when(() -> SetupIntent.create(any(SetupIntentCreateParams.class), any(RequestOptions.class)))
                    .thenReturn(intent);

            assertThat(adapter.create("user-1"))
                    .extracting("setupIntentId", "clientSecret")
                    .containsExactly("si_1", "secret");
        }
    }

    @Test
    void completeReturnsNormalizedCardDetails() {
        try (MockedStatic<SetupIntent> setupIntents = Mockito.mockStatic(SetupIntent.class);
                MockedStatic<com.stripe.model.PaymentMethod> paymentMethods =
                        Mockito.mockStatic(com.stripe.model.PaymentMethod.class)) {
            SetupIntent intent = mock(SetupIntent.class);
            when(intent.getMetadata()).thenReturn(Map.of("userId", "user-1"));
            when(intent.getStatus()).thenReturn("succeeded");
            when(intent.getPaymentMethod()).thenReturn("pm_1");
            setupIntents.when(() -> SetupIntent.retrieve(eq("si_1"), any(RequestOptions.class))).thenReturn(intent);

            com.stripe.model.PaymentMethod method = mock(com.stripe.model.PaymentMethod.class);
            com.stripe.model.PaymentMethod.Card card = mock(com.stripe.model.PaymentMethod.Card.class);
            when(method.getCard()).thenReturn(card);
            when(card.getBrand()).thenReturn("american express");
            when(card.getLast4()).thenReturn("4242");
            paymentMethods.when(() -> com.stripe.model.PaymentMethod.retrieve(eq("pm_1"), any(RequestOptions.class)))
                    .thenReturn(method);

            assertThat(adapter.complete("user-1", "si_1"))
                    .extracting("provider", "last4", "paymentMethodId")
                    .containsExactly("AMERICAN_EXPRESS", "4242", "pm_1");
        }
    }

    @Test
    void missingApiKeyFailsBeforeCallingStripe() {
        StripeSetupIntentAdapter unconfigured =
                new StripeSetupIntentAdapter(new StripeProperties(true, "", ""));

        assertThatThrownBy(() -> unconfigured.create("user-1"))
                .isInstanceOf(PaymentException.class);
    }

    @Test
    void createMapsStripeFailure() {
        try (MockedStatic<SetupIntent> setupIntents = Mockito.mockStatic(SetupIntent.class)) {
            setupIntents.when(() -> SetupIntent.create(any(SetupIntentCreateParams.class), any(RequestOptions.class)))
                    .thenThrow(mock(StripeException.class));

            assertThatThrownBy(() -> adapter.create("user-1")).isInstanceOf(PaymentException.class);
        }
    }

    @Test
    void completeRejectsWrongOwner() {
        try (MockedStatic<SetupIntent> setupIntents = Mockito.mockStatic(SetupIntent.class)) {
            SetupIntent intent = mock(SetupIntent.class);
            when(intent.getMetadata()).thenReturn(Map.of("userId", "other"));
            setupIntents.when(() -> SetupIntent.retrieve(eq("si_1"), any(RequestOptions.class))).thenReturn(intent);

            assertThatThrownBy(() -> adapter.complete("user-1", "si_1")).isInstanceOf(PaymentException.class);
        }
    }

    @Test
    void completeRejectsNonSucceededIntent() {
        try (MockedStatic<SetupIntent> setupIntents = Mockito.mockStatic(SetupIntent.class)) {
            SetupIntent intent = mock(SetupIntent.class);
            when(intent.getMetadata()).thenReturn(Map.of("userId", "user-1"));
            when(intent.getStatus()).thenReturn("processing");
            setupIntents.when(() -> SetupIntent.retrieve(eq("si_1"), any(RequestOptions.class))).thenReturn(intent);

            assertThatThrownBy(() -> adapter.complete("user-1", "si_1")).isInstanceOf(PaymentException.class);
        }
    }
}
