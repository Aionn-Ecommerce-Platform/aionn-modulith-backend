package com.aionn.payment.infrastructure.provider;

import com.aionn.payment.infrastructure.provider.config.StripeConnectProperties;
import com.aionn.payment.domain.exception.PaymentException;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StripeConnectAdapterTest {

    private final StripeConnectAdapter adapter =
            new StripeConnectAdapter(new StripeConnectProperties("https://refresh", "https://return"));

    @Test
    void createsExpressAccount() {
        try (MockedStatic<Account> accounts = Mockito.mockStatic(Account.class)) {
            Account account = mock(Account.class);
            when(account.getId()).thenReturn("acct_1");
            accounts.when(() -> Account.create(any(AccountCreateParams.class))).thenReturn(account);

            assertThat(adapter.createExpressAccount("merchant-1")).isEqualTo("acct_1");
        }
    }

    @Test
    void createsOnboardingLink() {
        try (MockedStatic<AccountLink> links = Mockito.mockStatic(AccountLink.class)) {
            AccountLink link = mock(AccountLink.class);
            when(link.getUrl()).thenReturn("https://stripe/onboard");
            links.when(() -> AccountLink.create(any(AccountLinkCreateParams.class))).thenReturn(link);

            assertThat(adapter.createOnboardingLink("acct_1")).isEqualTo("https://stripe/onboard");
        }
    }

    @Test
    void fetchesAccountCapabilities() {
        try (MockedStatic<Account> accounts = Mockito.mockStatic(Account.class)) {
            Account account = mock(Account.class);
            when(account.getId()).thenReturn("acct_1");
            when(account.getMetadata()).thenReturn(Map.of("merchantId", "merchant-1"));
            when(account.getChargesEnabled()).thenReturn(true);
            when(account.getPayoutsEnabled()).thenReturn(false);
            accounts.when(() -> Account.retrieve("acct_1")).thenReturn(account);

            assertThat(adapter.fetchAccountCapabilities("acct_1")).get()
                    .extracting("stripeAccountId", "merchantId", "chargesEnabled", "payoutsEnabled")
                    .containsExactly("acct_1", "merchant-1", true, false);
        }
    }

    @Test
    void mapsStripeFailureWhenCreatingAccount() {
        try (MockedStatic<Account> accounts = Mockito.mockStatic(Account.class)) {
            accounts.when(() -> Account.create(any(AccountCreateParams.class)))
                    .thenThrow(mock(StripeException.class));

            assertThatThrownBy(() -> adapter.createExpressAccount("merchant-1"))
                    .isInstanceOf(PaymentException.class);
        }
    }
}
