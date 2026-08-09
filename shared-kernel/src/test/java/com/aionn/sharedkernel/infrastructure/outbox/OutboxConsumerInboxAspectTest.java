package com.aionn.sharedkernel.infrastructure.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aionn.sharedkernel.domain.model.DomainEvent;
import com.aionn.sharedkernel.domain.model.EventEnvelope;
import com.aionn.sharedkernel.integration.event.IntegrationEvent;
import java.lang.reflect.Method;
import java.time.Instant;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

class OutboxConsumerInboxAspectTest {

    private static final String CONSUMER_ID = TestListener.class.getName() + "#onEvent("
            + TestEvent.class.getName() + ")";

    private final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final TransactionStatus transactionStatus = mock(TransactionStatus.class);
    private final OutboxConsumerInboxAspect aspect =
            new OutboxConsumerInboxAspect(repository, transactionManager);

    @BeforeEach
    void configureTransaction() {
        when(transactionManager.getTransaction(org.mockito.ArgumentMatchers.any())).thenReturn(transactionStatus);
    }

    @Test
    void recordsThisConsumerOnlyAfterSuccessfulHandling() throws Throwable {
        TestEvent event = new TestEvent("event-1", Instant.parse("2026-01-01T00:00:00Z"));
        ProceedingJoinPoint joinPoint = joinPoint(event, "handled");

        assertEquals("handled", aspect.consumeOnce(joinPoint));

        verify(joinPoint).proceed();
        verify(repository).markProcessed(CONSUMER_ID, event.eventId());
        verify(transactionManager).commit(transactionStatus);
    }

    @Test
    void skipsOnlyThisConsumerWhenItsInboxEntryExists() throws Throwable {
        TestEvent event = new TestEvent("event-2", Instant.parse("2026-01-01T00:00:00Z"));
        ProceedingJoinPoint joinPoint = joinPoint(event, "ignored");
        when(repository.wasProcessed(CONSUMER_ID, event.eventId())).thenReturn(true);

        aspect.consumeOnce(joinPoint);

        verify(joinPoint, never()).proceed();
        verify(repository, never()).markProcessed(CONSUMER_ID, event.eventId());
    }

    @Test
    void rollsBackWithoutInboxMarkerWhenListenerFails() throws Throwable {
        TestEvent event = new TestEvent("event-3", Instant.parse("2026-01-01T00:00:00Z"));
        ProceedingJoinPoint joinPoint = joinPoint(event, null);
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("temporary"));

        assertThrows(IllegalStateException.class, () -> aspect.consumeOnce(joinPoint));

        verify(repository, never()).markProcessed(CONSUMER_ID, event.eventId());
        verify(transactionManager).rollback(transactionStatus);
    }

    @Test
    void tracksADomainEventByItsEnvelopeIdentifier() throws Throwable {
        Instant occurredAt = Instant.parse("2026-01-01T00:00:00Z");
        EventEnvelope envelope = new EventEnvelope("event-4", "Order", "order-1",
                new TestDomainEvent(occurredAt), occurredAt);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = EnvelopeListener.class.getDeclaredMethod("onEnvelope", EventEnvelope.class);
        when(joinPoint.getArgs()).thenReturn(new Object[] { envelope });
        when(joinPoint.getTarget()).thenReturn(new EnvelopeListener());
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn("handled");

        assertEquals("handled", aspect.consumeOnce(joinPoint));

        verify(repository).markProcessed(EnvelopeListener.class.getName() + "#onEnvelope("
                + EventEnvelope.class.getName() + ")", "event-4");
    }

    @Test
    void bypassesTheInboxWhenTheArgumentCarriesNoEventIdentifier() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[] { "not-an-event" });
        when(joinPoint.proceed()).thenReturn("handled");

        assertEquals("handled", aspect.consumeOnce(joinPoint));

        verify(joinPoint).proceed();
        verify(repository, never()).wasProcessed(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
        verify(repository, never()).markProcessed(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void bypassesTheInboxWhenTheListenerDoesNotTakeExactlyOneArgument() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenReturn("handled");

        assertEquals("handled", aspect.consumeOnce(joinPoint));

        verify(joinPoint).proceed();
        verify(repository, never()).markProcessed(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    private static ProceedingJoinPoint joinPoint(TestEvent event, Object result) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = TestListener.class.getDeclaredMethod("onEvent", TestEvent.class);
        when(joinPoint.getArgs()).thenReturn(new Object[] { event });
        when(joinPoint.getTarget()).thenReturn(new TestListener());
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn(result);
        return joinPoint;
    }

    private static final class TestListener {
        void onEvent(TestEvent event) {
        }
    }

    private static final class EnvelopeListener {
        void onEnvelope(EventEnvelope envelope) {
        }
    }

    private record TestEvent(String eventId, Instant occurredAt) implements IntegrationEvent {
    }

    private record TestDomainEvent(Instant occurredAt) implements DomainEvent {
    }
}
