package com.aionn.chat.infrastructure.listener;

import com.aionn.chat.application.port.out.ConversationPersistencePort;
import com.aionn.chat.application.port.out.PresenceTracker;
import com.aionn.chat.application.port.out.observability.ChatMetricsPort;
import com.aionn.chat.domain.event.ChatEvents;
import com.aionn.chat.domain.model.Conversation;
import com.aionn.sharedkernel.integration.publisher.IntegrationEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MessageSentListenerTest {

    @Test
    void queuesOfflineNotificationInsideWritableTransaction() {
        ConversationPersistencePort conversations = mock(ConversationPersistencePort.class);
        PresenceTracker presence = mock(PresenceTracker.class);
        IntegrationEventPublisher publisher = mock(IntegrationEventPublisher.class);
        ChatMetricsPort metrics = mock(ChatMetricsPort.class);
        Conversation conversation = mock(Conversation.class);
        when(conversations.findById("conversation")).thenReturn(Optional.of(conversation));
        when(conversation.getParticipants()).thenReturn(List.of());
        when(presence.filterOnline(Set.of("recipient"))).thenReturn(Set.of());
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
            return null;
        }).when(publisher).publish(any());

        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        MessageSentListener target = new MessageSentListener(conversations, presence, publisher, metrics,
                Clock.fixed(now, ZoneOffset.UTC));
        ProxyFactory factory = new ProxyFactory(target);
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(new TestTransactionManager());
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        factory.addAdvice(interceptor);
        MessageSentListener listener = (MessageSentListener) factory.getProxy();

        listener.onMessageSent(new ChatEvents.MessageSent("conversation", "message", "sender", null,
                List.of("recipient"), null, null, now));

        verify(publisher).publish(any());
        verify(metrics).pushNotificationDispatched("queued");
    }

    private static class TestTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            // No resources to open; the superclass manages the transaction state asserted by this test.
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            // No backing resource to commit; this test only checks the transaction surrounding publication.
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            // No backing resource to roll back; the superclass still clears the thread-bound transaction state.
        }
    }
}
