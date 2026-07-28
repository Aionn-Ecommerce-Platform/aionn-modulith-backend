package com.aionn.chat.infrastructure.observability;

import com.aionn.chat.application.port.out.observability.ChatMetricsPort;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerChatMetricsAdapter implements ChatMetricsPort {

    private final MeterRegistry registry;

    public MicrometerChatMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void messageSent(String messageType) {
        registry.counter("chat.message.sent", "type", messageType).increment();
    }

    @Override
    public void conversationStarted() {
        registry.counter("chat.conversation.started").increment();
    }

    @Override
    public void messageRecalled() {
        registry.counter("chat.message.recalled").increment();
    }

    @Override
    public void userBlocked() {
        registry.counter("chat.user.blocked").increment();
    }

    @Override
    public void pushNotificationDispatched(String outcome) {
        registry.counter("chat.push.dispatched", "outcome", outcome).increment();
    }

    @Override
    public void autoReplyTriggered() {
        registry.counter("chat.autoreply.triggered").increment();
    }
}
