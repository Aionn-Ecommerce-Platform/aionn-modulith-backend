package com.aionn.identity.infrastructure.integration;
import com.aionn.identity.infrastructure.integration.identity.IdentityIntegrationEventPublisher;

import com.aionn.sharedkernel.integration.event.IntegrationEvent;
import com.aionn.sharedkernel.integration.event.identity.EmailChangedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.identity.PasswordChangedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.identity.PhoneChangedIntegrationEvent;
import com.aionn.sharedkernel.integration.publisher.IntegrationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IdentityIntegrationEventPublisherTest {

    @Mock
    private IntegrationEventPublisher integrationEventPublisher;

    private IdentityIntegrationEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new IdentityIntegrationEventPublisher(integrationEventPublisher, java.time.Clock.systemUTC());
    }

    @Test
    void publishPasswordChangedEmitsEvent() {
        ArgumentCaptor<IntegrationEvent> captor = ArgumentCaptor.captor();

        publisher.publishPasswordChanged("user-1", "EMAIL");

        verify(integrationEventPublisher).publish(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(PasswordChangedIntegrationEvent.class);
        PasswordChangedIntegrationEvent event = (PasswordChangedIntegrationEvent) captor.getValue();
        assertThat(event.userId()).isEqualTo("user-1");
        assertThat(event.channelHint()).isEqualTo("EMAIL");
        assertThat(event.eventId()).isNotBlank();
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void publishEmailChangedEmitsEvent() {
        ArgumentCaptor<IntegrationEvent> captor = ArgumentCaptor.captor();

        publisher.publishEmailChanged("user-1", "old@example.com", "new@example.com");

        verify(integrationEventPublisher).publish(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(EmailChangedIntegrationEvent.class);
        EmailChangedIntegrationEvent event = (EmailChangedIntegrationEvent) captor.getValue();
        assertThat(event.userId()).isEqualTo("user-1");
        assertThat(event.oldEmail()).isEqualTo("old@example.com");
        assertThat(event.newEmail()).isEqualTo("new@example.com");
    }

    @Test
    void publishPhoneChangedEmitsEvent() {
        ArgumentCaptor<IntegrationEvent> captor = ArgumentCaptor.captor();

        publisher.publishPhoneChanged("user-1", "0900000000", "0911111111");

        verify(integrationEventPublisher).publish(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(PhoneChangedIntegrationEvent.class);
        PhoneChangedIntegrationEvent event = (PhoneChangedIntegrationEvent) captor.getValue();
        assertThat(event.userId()).isEqualTo("user-1");
        assertThat(event.oldPhone()).isEqualTo("0900000000");
        assertThat(event.newPhone()).isEqualTo("0911111111");
    }
}
