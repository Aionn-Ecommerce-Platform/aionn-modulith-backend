package com.aionn.notification.infrastructure.channel;

import com.aionn.notification.domain.valueobject.NotificationChannel;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TwilioSmsSenderTest {

    private static TwilioSmsSender sender(String sid, String token, String from) {
        TwilioSmsSender sender = new TwilioSmsSender();
        ReflectionTestUtils.setField(sender, "accountSid", sid);
        ReflectionTestUtils.setField(sender, "authToken", token);
        ReflectionTestUtils.setField(sender, "fromPhoneNumber", from);
        return sender;
    }

    @Test
    void exposesSmsChannel() {
        assertThat(sender("sid", "token", "+100").channel()).isEqualTo(NotificationChannel.SMS);
    }

    @Test
    void initRejectsMissingAccountSid() {
        TwilioSmsSender sender = sender("  ", "token", "+100");

        assertThatThrownBy(sender::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TWILIO_ACCOUNT_SID");
    }

    @Test
    void initRejectsMissingAuthToken() {
        TwilioSmsSender sender = sender("sid", null, "+100");

        assertThatThrownBy(sender::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TWILIO_AUTH_TOKEN");
    }

    @Test
    void initRejectsMissingFromPhoneNumber() {
        TwilioSmsSender sender = sender("sid", "token", "");

        assertThatThrownBy(sender::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TWILIO_FROM_PHONE_NUMBER");
    }
}
