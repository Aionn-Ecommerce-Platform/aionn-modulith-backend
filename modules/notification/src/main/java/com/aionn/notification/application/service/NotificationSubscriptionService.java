package com.aionn.notification.application.service;

import com.aionn.notification.application.dto.subscription.command.SubscriptionCommands;
import com.aionn.notification.application.dto.subscription.result.DeviceTokenResult;
import com.aionn.notification.application.dto.subscription.result.SubscriptionResult;
import com.aionn.notification.application.mapper.NotificationResultMapper;
import com.aionn.notification.application.port.out.DeviceTokenPersistencePort;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.notification.application.port.out.NotificationSubscriptionPersistencePort;
import com.aionn.notification.domain.exception.NotificationErrorCode;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.model.DeviceToken;
import com.aionn.notification.domain.model.NotificationSubscription;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationSubscriptionService {

    private final NotificationSubscriptionPersistencePort subscriptionRepository;
    private final DeviceTokenPersistencePort deviceTokenRepository;
    private final NotificationResultMapper mapper;
    private final EventPublisher eventPublisher;

    public SubscriptionResult updateChannel(SubscriptionCommands.UpdateChannel command) {
        NotificationSubscription s = subscriptionRepository.findByUserId(command.userId())
                .orElseGet(() -> NotificationSubscription.createDefault(command.userId()));
        s.update(command.category(), command.channel(), command.enabled());
        NotificationSubscription saved = subscriptionRepository.save(s);
        eventPublisher.publish(s.pullEvents());
        return mapper.toResult(saved);
    }

    @Transactional(readOnly = true)
    public SubscriptionResult get(String userId) {
        NotificationSubscription s = subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> NotificationSubscription.createDefault(userId));
        return mapper.toResult(s);
    }

    public DeviceTokenResult registerDeviceToken(SubscriptionCommands.RegisterDeviceToken command) {
        var existing = deviceTokenRepository.findByUserAndToken(command.userId(), command.deviceToken());
        if (existing.isPresent()) {
            return mapper.toResult(existing.get());
        }
        DeviceToken token = DeviceToken.register(IdGenerator.ulid(),
                command.userId(), command.deviceToken(), command.os());
        DeviceToken saved = deviceTokenRepository.save(token);
        eventPublisher.publish(token.pullEvents());
        return mapper.toResult(saved);
    }

    public void removeDeviceToken(SubscriptionCommands.RemoveDeviceToken command) {
        DeviceToken token = deviceTokenRepository.findById(command.tokenId())
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.DEVICE_TOKEN_NOT_FOUND));
        if (!token.getUserId().equals(command.userId())) {
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_FORBIDDEN);
        }
        token.deactivate();
        deviceTokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public List<DeviceTokenResult> listDeviceTokens(String userId) {
        return deviceTokenRepository.findActiveByUserId(userId).stream().map(mapper::toResult).toList();
    }
}

