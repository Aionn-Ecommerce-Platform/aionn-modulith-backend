package com.aionn.notification.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "notification.sms")
public record NotificationSmsProperties(
        @DefaultValue("logging") String provider,
        @DefaultValue Twilio twilio) {

    public record Twilio(
            @DefaultValue("") String accountSid,
            @DefaultValue("") String authToken,
            @DefaultValue("") String fromPhoneNumber) {
    }
}
