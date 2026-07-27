package com.aionn.notification.infrastructure.channel;

import com.aionn.notification.application.port.out.ChannelSender;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChannelSenderTest {

    private static final ChannelSender.DeliveryRequest REQUEST = new ChannelSender.DeliveryRequest(
            "noti-1", "user-1", "to@example.com", "Subject", "Content body");

    @Mock
    private JavaMailSender mailSender;

    @Test
    void inAppSenderReportsSuccessWithNotificationId() {
        InAppSender sender = new InAppSender();

        ChannelSender.DeliveryResult result = sender.send(REQUEST);

        assertThat(sender.channel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(result.success()).isTrue();
        assertThat(result.externalId()).isEqualTo("in-app:noti-1");
    }

    @Test
    void loggingEmailSenderReportsSuccess() {
        LoggingEmailSender sender = new LoggingEmailSender();

        ChannelSender.DeliveryResult result = sender.send(REQUEST);

        assertThat(sender.channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(result.success()).isTrue();
        assertThat(result.externalId()).startsWith("email-");
    }

    @Test
    void loggingSmsSenderReportsSuccess() {
        LoggingSmsSender sender = new LoggingSmsSender();

        ChannelSender.DeliveryResult result = sender.send(REQUEST);

        assertThat(sender.channel()).isEqualTo(NotificationChannel.SMS);
        assertThat(result.externalId()).startsWith("sms-");
    }

    @Test
    void loggingPushSenderReportsSuccess() {
        LoggingPushSender sender = new LoggingPushSender();

        ChannelSender.DeliveryResult result = sender.send(REQUEST);

        assertThat(sender.channel()).isEqualTo(NotificationChannel.PUSH);
        assertThat(result.externalId()).startsWith("push-");
    }

    @Test
    void loggingSendersToleratePayloadWithoutContent() {
        ChannelSender.DeliveryRequest empty = new ChannelSender.DeliveryRequest(
                "noti-2", "user-1", "to@example.com", null, null);

        assertThat(new LoggingEmailSender().send(empty).success()).isTrue();
        assertThat(new LoggingSmsSender().send(empty).success()).isTrue();
        assertThat(new LoggingPushSender().send(empty).success()).isTrue();
    }

    @Test
    void smtpSenderSendsMessageWithConfiguredFromAddress() {
        SmtpEmailSender sender = new SmtpEmailSender(mailSender);
        ReflectionTestUtils.setField(sender, "fromAddress", "no-reply@aionn.local");

        ChannelSender.DeliveryResult result = sender.send(REQUEST);

        assertThat(sender.channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(result.success()).isTrue();
        assertThat(result.externalId()).isEqualTo("smtp:noti-1");

        SimpleMailMessage expected = new SimpleMailMessage();
        expected.setFrom("no-reply@aionn.local");
        expected.setTo("to@example.com");
        expected.setSubject("Subject");
        expected.setText("Content body");
        verify(mailSender).send(expected);
    }

    @Test
    void smtpSenderOmitsFromWhenNotConfigured() {
        SmtpEmailSender sender = new SmtpEmailSender(mailSender);
        ReflectionTestUtils.setField(sender, "fromAddress", "  ");

        assertThat(sender.send(REQUEST).success()).isTrue();

        SimpleMailMessage expected = new SimpleMailMessage();
        expected.setTo("to@example.com");
        expected.setSubject("Subject");
        expected.setText("Content body");
        verify(mailSender).send(expected);
    }

    @Test
    void smtpSenderFallsBackToDefaultSubject() {
        SmtpEmailSender sender = new SmtpEmailSender(mailSender);
        ReflectionTestUtils.setField(sender, "fromAddress", null);

        sender.send(new ChannelSender.DeliveryRequest("noti-3", "user-1", "to@example.com",
                "  ", "Content body"));

        SimpleMailMessage expected = new SimpleMailMessage();
        expected.setTo("to@example.com");
        expected.setSubject("Aionn notification");
        expected.setText("Content body");
        verify(mailSender).send(expected);
    }

    @Test
    void smtpSenderTranslatesMailExceptionIntoFailure() {
        SmtpEmailSender sender = new SmtpEmailSender(mailSender);
        ReflectionTestUtils.setField(sender, "fromAddress", "no-reply@aionn.local");
        doThrow(new MailSendException("relay refused")).when(mailSender).send(any(SimpleMailMessage.class));

        ChannelSender.DeliveryResult result = sender.send(REQUEST);

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("MAIL_SEND_FAILED");
        assertThat(result.errorReason()).contains("relay refused");
    }
}
