package com.aionn.notification.infrastructure.channel;

import com.aionn.notification.application.port.out.ChannelSender;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "notification.sms", name = "provider", havingValue = "twilio")
public class TwilioSmsSender implements ChannelSender {

    @Value("${TWILIO_ACCOUNT_SID:}")
    private String accountSid;

    @Value("${TWILIO_AUTH_TOKEN:}")
    private String authToken;

    @Value("${TWILIO_FROM_PHONE_NUMBER:}")
    private String fromPhoneNumber;

    @PostConstruct
    void init() {
        requireNotBlank(accountSid, "TWILIO_ACCOUNT_SID");
        requireNotBlank(authToken, "TWILIO_AUTH_TOKEN");
        requireNotBlank(fromPhoneNumber, "TWILIO_FROM_PHONE_NUMBER");
        Twilio.init(accountSid, authToken);
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public DeliveryResult send(DeliveryRequest request) {
        try {
            Message message = Message.creator(
                    new PhoneNumber(request.to()),
                    new PhoneNumber(fromPhoneNumber),
                    request.content())
                    .create();
            return DeliveryResult.ok(message.getSid());
        } catch (RuntimeException ex) {
            log.warn("Failed to send SMS notification {} to {}", request.notiId(), request.to(), ex);
            return DeliveryResult.failed("SMS_SEND_FAILED", ex.getMessage());
        }
    }

    private static void requireNotBlank(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required configuration: " + propertyName);
        }
    }
}
