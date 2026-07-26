package com.aionn.notification.application.port.out;

import com.aionn.notification.domain.valueobject.NotificationChannel;

public interface ChannelSender {

    NotificationChannel channel();

    DeliveryResult send(DeliveryRequest request);

    record DeliveryRequest(
            String notiId,
            String userId,
            String to,
            String subject,
            String content) {
    }

    record DeliveryResult(boolean success, String externalId, String errorCode, String errorReason) {
        public static DeliveryResult ok(String externalId) {
            return new DeliveryResult(true, externalId, null, null);
        }

        public static DeliveryResult failed(String code, String reason) {
            return new DeliveryResult(false, null, code, reason);
        }
    }
}

