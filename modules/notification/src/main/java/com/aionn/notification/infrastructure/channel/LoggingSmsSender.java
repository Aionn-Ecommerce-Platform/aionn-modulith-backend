package com.aionn.notification.infrastructure.channel;

import com.aionn.notification.application.port.out.ChannelSender;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "notification.sms", name = "provider", havingValue = "logging")
public class LoggingSmsSender implements ChannelSender {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public DeliveryResult send(DeliveryRequest request) {
        // Don't log content - SMS often carries OTPs / reset tokens.
        log.info("[SMS] to={} content-len={}",
                request.to(), request.content() == null ? 0 : request.content().length());
        return DeliveryResult.ok("sms-" + IdGenerator.ulid());
    }
}
