package com.aionn.notification.infrastructure.channel;

import com.aionn.notification.application.port.out.ChannelSender;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import org.springframework.stereotype.Component;

@Component
public class InAppSender implements ChannelSender {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public DeliveryResult send(DeliveryRequest request) {
        return DeliveryResult.ok("in-app:" + request.notiId());
    }
}

