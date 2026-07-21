package com.aionn.payment.application.service;

import com.aionn.payment.application.dto.method.command.LinkMethodCommand;
import com.aionn.payment.application.dto.method.command.RemoveMethodCommand;
import com.aionn.payment.application.dto.method.command.VerifyMethodCommand;
import com.aionn.payment.application.dto.method.result.PaymentMethodResult;
import com.aionn.payment.application.mapper.PaymentResultMapper;
import com.aionn.payment.application.port.out.PaymentMethodPersistencePort;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.model.PaymentMethod;
import com.aionn.payment.domain.valueobject.PaymentMethodStatus;
import com.aionn.payment.infrastructure.provider.config.StripeProperties;
import com.aionn.sharedkernel.application.port.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentMethodServiceTest {

    @Mock
    private PaymentMethodPersistencePort repository;
    @Mock
    private EventPublisher eventPublisher;

    private final Instant fixedInstant = Instant.parse("2026-01-01T00:00:00Z");
    private final Clock clock = Clock.fixed(fixedInstant, java.time.ZoneOffset.UTC);

    private PaymentResultMapper mapper;
    private PaymentMethodService service;
    private PaymentMethodService serviceWithKey;

    @BeforeEach
    void setUp() {
        mapper = new PaymentResultMapper();
        StripeProperties stripeProperties = new StripeProperties("", "");
        service = new PaymentMethodService(repository, mapper, eventPublisher, stripeProperties, clock);

        StripeProperties keyedProperties = new StripeProperties("sk_test_fake_key_for_test", "");
        serviceWithKey = new PaymentMethodService(repository, mapper, eventPublisher, keyedProperties, clock);
    }

    @Test
    void linkPersistsMethodAndReturnsResult() {
        when(repository.save(any(PaymentMethod.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentMethodResult result = service.link(
                new LinkMethodCommand("u1", "stripe", "4242", "tok-abc"));

        assertThat(result.userId()).isEqualTo("u1");
        assertThat(result.provider()).isEqualTo("stripe");
        assertThat(result.status()).isEqualTo("LINKED");
        assertThat(result.last4Digits()).isEqualTo("4242");
        verify(repository).save(any(PaymentMethod.class));
        verify(eventPublisher).publish(anyCollection());
    }

    @Test
    void verifyTransitionsToVerified() {
        PaymentMethod existing = PaymentMethod.link("m1", "u1", "stripe", "4242", "tok-abc", fixedInstant);
        when(repository.findById("m1")).thenReturn(Optional.of(existing));
        when(repository.save(any(PaymentMethod.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentMethodResult result = service.verify(new VerifyMethodCommand("u1", "m1"));

        assertThat(result.status()).isEqualTo("VERIFIED");
        ArgumentCaptor<PaymentMethod> captor = ArgumentCaptor.forClass(PaymentMethod.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentMethodStatus.VERIFIED);
    }

    @Test
    void verifyOtherUserMethodForbidden() {
        PaymentMethod existing = PaymentMethod.link("m1", "u1", "stripe", "4242", "tok-abc", fixedInstant);
        when(repository.findById("m1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.verify(new VerifyMethodCommand("OTHER", "m1")))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.METHOD_FORBIDDEN.getCode());
    }

    @Test
    void verifyMissingMethodThrowsNotFound() {
        when(repository.findById("m-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify(new VerifyMethodCommand("u1", "m-missing")))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.METHOD_NOT_FOUND.getCode());
    }

    @Test
    void removeMarksMethodRemoved() {
        PaymentMethod existing = PaymentMethod.link("m1", "u1", "stripe", "4242", "tok-abc", fixedInstant);
        when(repository.findById("m1")).thenReturn(Optional.of(existing));
        when(repository.save(any(PaymentMethod.class))).thenAnswer(inv -> inv.getArgument(0));

        service.remove(new RemoveMethodCommand("u1", "m1"));

        ArgumentCaptor<PaymentMethod> captor = ArgumentCaptor.forClass(PaymentMethod.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentMethodStatus.REMOVED);
    }

    @Test
    void listMineReturnsActiveMethods() {
        PaymentMethod m1 = PaymentMethod.link("m1", "u1", "stripe", "4242", "tok-1", fixedInstant);
        PaymentMethod m2 = PaymentMethod.link("m2", "u1", "stripe", "1111", "tok-2", fixedInstant);
        when(repository.findActiveByUserId("u1")).thenReturn(List.of(m1, m2));

        List<PaymentMethodResult> result = service.listMine("u1");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PaymentMethodResult::methodId).containsExactly("m1", "m2");
    }

    @Test
    void createStripeSetupIntentMissingApiKeyThrows() {
        assertThatThrownBy(() -> service.createStripeSetupIntent("u1"))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_GATEWAY_ERROR.getCode());
    }

    @Test
    void getMethodReturnsCorrectResult() {
        PaymentMethod existing = PaymentMethod.link("m1", "u1", "stripe", "4242", "tok-abc", fixedInstant);
        when(repository.findById("m1")).thenReturn(Optional.of(existing));

        PaymentMethodResult result = service.get("u1", "m1");

        assertThat(result.methodId()).isEqualTo("m1");
        assertThat(result.userId()).isEqualTo("u1");
    }

    @Test
    void getMethodOtherUserThrowsForbidden() {
        PaymentMethod existing = PaymentMethod.link("m1", "u1", "stripe", "4242", "tok-abc", fixedInstant);
        when(repository.findById("m1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.get("OTHER", "m1"))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.METHOD_FORBIDDEN.getCode());
    }

    @Test
    void completeStripeSetupIntentMissingApiKeyThrows() {
        assertThatThrownBy(() -> service.completeStripeSetupIntent("u1", "si_123"))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_GATEWAY_ERROR.getCode());
    }

    @Test
    void listForUserCallsListMine() {
        PaymentMethod m1 = PaymentMethod.link("m1", "u1", "stripe", "4242", "tok-1", fixedInstant);
        when(repository.findActiveByUserId("u1")).thenReturn(List.of(m1));

        List<PaymentMethodResult> result = service.listMine("u1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).methodId()).isEqualTo("m1");
    }

    @Test
    void removeOtherUserMethodThrowsForbidden() {
        PaymentMethod existing = PaymentMethod.link("m1", "u1", "stripe", "4242", "tok-abc", fixedInstant);
        when(repository.findById("m1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.remove(new RemoveMethodCommand("OTHER", "m1")))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.METHOD_FORBIDDEN.getCode());
    }

    @Test
    void removeMissingMethodThrowsNotFound() {
        when(repository.findById("m-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remove(new RemoveMethodCommand("u1", "m-missing")))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.METHOD_NOT_FOUND.getCode());
    }

    @Test
    void getMissingMethodThrowsNotFound() {
        when(repository.findById("m-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("u1", "m-missing"))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.METHOD_NOT_FOUND.getCode());
    }

    @Test
    void createStripeSetupIntentWithValidKeyThrowsGatewayErrorOnStripeFailure() {
        assertThatThrownBy(() -> serviceWithKey.createStripeSetupIntent("u1"))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_GATEWAY_ERROR.getCode());
    }

    @Test
    void completeStripeSetupIntentWithValidKeyThrowsGatewayErrorOnStripeFailure() {
        assertThatThrownBy(() -> serviceWithKey.completeStripeSetupIntent("u1", "si_test"))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_GATEWAY_ERROR.getCode());
    }
}
