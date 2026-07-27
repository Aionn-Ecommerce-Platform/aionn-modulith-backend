package com.aionn.notification.infrastructure.channel;

import com.aionn.notification.application.port.out.ChannelSender;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "notification.email", name = "provider", havingValue = "logging")
public class LoggingEmailSender implements ChannelSender {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public DeliveryResult send(DeliveryRequest request) {
        log.info("[EMAIL] to={} subject={} body-len={}",
                request.to(), request.subject(), request.content() == null ? 0 : request.content().length());
        return DeliveryResult.ok("email-" + IdGenerator.ulid());
    }
}
