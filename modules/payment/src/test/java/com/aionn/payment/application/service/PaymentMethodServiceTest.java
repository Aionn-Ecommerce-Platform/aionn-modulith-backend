package com.aionn.payment.application.service;

import com.aionn.payment.application.dto.method.command.LinkMethodCommand;
import com.aionn.payment.application.dto.method.command.RemoveMethodCommand;
import com.aionn.payment.application.dto.method.command.VerifyMethodCommand;
import com.aionn.payment.application.port.out.PaymentMethodPersistencePort;
import com.aionn.payment.application.port.out.StripeSetupIntentPort;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.model.PaymentMethod;
import com.aionn.payment.domain.valueobject.PaymentMethodStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

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
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PaymentMethodServiceTest {

    @Mock
    private PaymentMethodPersistencePort repository;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private StripeSetupIntentPort stripeSetupIntentPort;
    @Mock
    private TransactionTemplate transactionTemplate;

    private final Instant fixedInstant = Instant.parse("2026-01-01T00:00:00Z");
    private final Clock clock = Clock.fixed(fixedInstant, java.time.ZoneOffset.UTC);

    private PaymentMethodService service;

    @BeforeEach
    void setUp() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(null));
        service = new PaymentMethodService(repository, eventPublisher, stripeSetupIntentPort, clock,
                transactionTemplate);
    }

    @Test
    void linkPersistsMethodAndReturnsEntity() {
        when(repository.save(any(PaymentMethod.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentMethod result = service.link(
                new LinkMethodCommand("u1", "stripe", "4242", "tok-abc"));

        assertThat(result.getUserId()).isEqualTo("u1");
        assertThat(result.getProvider()).isEqualTo("stripe");
        assertThat(result.getStatus()).isEqualTo(PaymentMethodStatus.LINKED);
        assertThat(result.getLast4Digits()).isEqualTo("4242");
        verify(repository).save(any(PaymentMethod.class));
        verify(eventPublisher).publish(anyCollection());
    }

    @Test
    void verifyTransitionsToVerified() {
        PaymentMethod existing = PaymentMethod.link("m1", "u1", "stripe", "4242", "tok-abc", fixedInstant);
        when(repository.findById("m1")).thenReturn(Optional.of(existing));
        when(repository.save(any(PaymentMethod.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentMethod result = service.verify(new VerifyMethodCommand("u1", "m1"));

        assertThat(result.getStatus()).isEqualTo(PaymentMethodStatus.VERIFIED);
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

        List<PaymentMethod> result = service.listMine("u1");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PaymentMethod::getMethodId).containsExactly("m1", "m2");
    }

    @Test
    void createStripeSetupIntentMissingApiKeyThrows() {
        when(stripeSetupIntentPort.create("u1"))
                .thenThrow(new PaymentException(PaymentErrorCode.PAYMENT_GATEWAY_ERROR, "missing key"));
        assertThatThrownBy(() -> service.createStripeSetupIntent("u1"))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_GATEWAY_ERROR.getCode());
    }

    @Test
    void getMethodReturnsCorrectEntity() {
        PaymentMethod existing = PaymentMethod.link("m1", "u1", "stripe", "4242", "tok-abc", fixedInstant);
        when(repository.findById("m1")).thenReturn(Optional.of(existing));

        PaymentMethod result = service.get("u1", "m1");

        assertThat(result.getMethodId()).isEqualTo("m1");
        assertThat(result.getUserId()).isEqualTo("u1");
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
        when(stripeSetupIntentPort.complete("u1", "si_123"))
                .thenThrow(new PaymentException(PaymentErrorCode.PAYMENT_GATEWAY_ERROR, "missing key"));
        assertThatThrownBy(() -> service.completeStripeSetupIntent("u1", "si_123"))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_GATEWAY_ERROR.getCode());
    }

    @Test
    void listForUserCallsListMine() {
        PaymentMethod m1 = PaymentMethod.link("m1", "u1", "stripe", "4242", "tok-1", fixedInstant);
        when(repository.findActiveByUserId("u1")).thenReturn(List.of(m1));

        List<PaymentMethod> result = service.listMine("u1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMethodId()).isEqualTo("m1");
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
    void createStripeSetupIntentDelegatesToGateway() {
        var expected = new com.aionn.payment.application.dto.method.result.StripeSetupIntentResult("si_1", "secret");
        when(stripeSetupIntentPort.create("u1")).thenReturn(expected);

        assertThat(service.createStripeSetupIntent("u1")).isEqualTo(expected);
    }

    @Test
    void completeStripeSetupIntentPersistsGatewayResultInTransaction() {
        when(stripeSetupIntentPort.complete("u1", "si_test"))
                .thenReturn(new StripeSetupIntentPort.CompletedSetupIntent("VISA", "4242", "pm_1"));
        when(repository.save(any(PaymentMethod.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentMethod result = service.completeStripeSetupIntent("u1", "si_test");

        assertThat(result.getProvider()).isEqualTo("VISA");
        assertThat(result.getLast4Digits()).isEqualTo("4242");
        assertThat(result.getStatus()).isEqualTo(PaymentMethodStatus.VERIFIED);
        verify(repository).save(any(PaymentMethod.class));
    }
}
