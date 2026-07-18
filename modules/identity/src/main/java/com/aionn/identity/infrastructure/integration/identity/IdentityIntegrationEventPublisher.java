package com.aionn.identity.infrastructure.integration.identity;

import com.aionn.identity.application.port.out.integration.IdentityIntegrationEventPublisherPort;
import com.aionn.sharedkernel.integration.event.identity.EmailChangedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.identity.PasswordChangedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.identity.PhoneChangedIntegrationEvent;
import com.aionn.sharedkernel.integration.publisher.IntegrationEventPublisher;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityIntegrationEventPublisher implements IdentityIntegrationEventPublisherPort {

    private final IntegrationEventPublisher integrationEventPublisher;
    private final Clock clock;

    @Override
    public void publishPasswordChanged(String userId, String channelHint) {
        log.debug("Publishing PasswordChangedIntegrationEvent for user: {}", userId);
        integrationEventPublisher.publish(new PasswordChangedIntegrationEvent(
                IdGenerator.ulid(), userId, channelHint, Instant.now(clock)));
    }

    @Override
    public void publishEmailChanged(String userId, String oldEmail, String newEmail) {
        log.debug("Publishing EmailChangedIntegrationEvent for user: {}", userId);
        integrationEventPublisher.publish(new EmailChangedIntegrationEvent(
                IdGenerator.ulid(), userId, oldEmail, newEmail, Instant.now(clock)));
    }

    @Override
    public void publishPhoneChanged(String userId, String oldPhone, String newPhone) {
        log.debug("Publishing PhoneChangedIntegrationEvent for user: {}", userId);
        integrationEventPublisher.publish(new PhoneChangedIntegrationEvent(
                IdGenerator.ulid(), userId, oldPhone, newPhone, Instant.now(clock)));
    }
}
