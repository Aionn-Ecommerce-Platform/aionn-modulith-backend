package com.aionn.chat.infrastructure.listener;

import com.aionn.chat.application.port.out.ConversationPersistencePort;
import com.aionn.chat.application.port.out.PresenceTracker;
import com.aionn.chat.application.port.out.observability.ChatMetricsPort;
import com.aionn.chat.domain.event.ChatEvents;
import com.aionn.chat.domain.model.Conversation;
import com.aionn.chat.domain.valueobject.Participant;
import com.aionn.sharedkernel.integration.event.chat.MessageSentIntegrationEvent;
import com.aionn.sharedkernel.integration.publisher.IntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSentListener {

    private final ConversationPersistencePort conversationRepository;
    private final PresenceTracker presenceTracker;
    private final IntegrationEventPublisher integrationEventPublisher;
    private final ChatMetricsPort chatMetrics;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onMessageSent(ChatEvents.MessageSent event) {
        if (event.recipientIds() == null || event.recipientIds().isEmpty()) {
            return;
        }

        Conversation conversation = conversationRepository.findById(event.conversationId()).orElse(null);
        if (conversation == null) {
            return;
        }

        Participant sender = conversation.getParticipants().stream()
                .filter(p -> p.userId().equals(event.senderId()))
                .findFirst()
                .orElse(null);
        String senderDisplayName = sender == null || sender.displayName() == null
                ? event.senderId()
                : sender.displayName();
        String preview = event.payload() == null || event.payload().body() == null
                ? ""
                : event.payload().body();

        Set<String> onlineRecipients = presenceTracker.filterOnline(new HashSet<>(event.recipientIds()));
        for (String recipientId : event.recipientIds()) {
            if (onlineRecipients.contains(recipientId)) {
                log.debug("Skipping push notification for online recipient {}", recipientId);
                continue;
            }

            MessageSentIntegrationEvent integrationEvent = new MessageSentIntegrationEvent(
                    null,
                    event.conversationId(),
                    event.messageId(),
                    event.senderId(),
                    recipientId,
                    senderDisplayName,
                    preview,
                    Instant.now());

            integrationEventPublisher.publish(integrationEvent);
            chatMetrics.pushNotificationDispatched("queued");
            log.debug("Published MessageSentIntegrationEvent for offline recipient {}", recipientId);
        }
    }
}
