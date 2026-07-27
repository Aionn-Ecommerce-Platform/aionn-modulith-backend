package com.aionn.notification.application.service;

import com.aionn.notification.application.dto.subscription.command.SubscriptionCommands;
import com.aionn.notification.application.port.out.DeviceTokenPersistencePort;
import com.aionn.notification.application.port.out.NotificationSubscriptionPersistencePort;
import com.aionn.notification.domain.exception.NotificationErrorCode;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.model.DeviceToken;
import com.aionn.notification.domain.model.NotificationSubscription;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationSubscriptionService {

    private final NotificationSubscriptionPersistencePort subscriptionRepository;
    private final DeviceTokenPersistencePort deviceTokenRepository;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public NotificationSubscription updateChannel(SubscriptionCommands.UpdateChannel command) {
        NotificationSubscription subscription = subscriptionRepository.findByUserId(command.userId())
                .orElseGet(() -> NotificationSubscription.createDefault(command.userId(), clock));
        subscription.update(command.category(), command.channel(), command.enabled(), clock);
        NotificationSubscription saved = subscriptionRepository.save(subscription);
        eventPublisher.publish(subscription.pullEvents());
        return saved;
    }

    @Transactional(readOnly = true)
    public NotificationSubscription get(String userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> NotificationSubscription.createDefault(userId, clock));
    }

    public DeviceToken registerDeviceToken(SubscriptionCommands.RegisterDeviceToken command) {
        return deviceTokenRepository.findByUserAndToken(command.userId(), command.deviceToken())
                .orElseGet(() -> {
                    DeviceToken token = DeviceToken.register(IdGenerator.ulid(),
                            command.userId(), command.deviceToken(), command.os(), clock);
                    DeviceToken saved = deviceTokenRepository.save(token);
                    eventPublisher.publish(token.pullEvents());
                    return saved;
                });
    }

    public void removeDeviceToken(SubscriptionCommands.RemoveDeviceToken command) {
        DeviceToken token = deviceTokenRepository.findById(command.tokenId())
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.DEVICE_TOKEN_NOT_FOUND));
        if (!token.getUserId().equals(command.userId())) {
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_FORBIDDEN);
        }
        token.deactivate(clock);
        deviceTokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public List<DeviceToken> listDeviceTokens(String userId) {
        return deviceTokenRepository.findActiveByUserId(userId);
    }
}
