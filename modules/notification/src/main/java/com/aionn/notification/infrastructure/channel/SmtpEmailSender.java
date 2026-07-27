package com.aionn.notification.infrastructure.channel;

import com.aionn.notification.application.port.out.ChannelSender;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "notification.email", name = "provider", havingValue = "smtp")
public class SmtpEmailSender implements ChannelSender {

    private final JavaMailSender mailSender;

    @Value("${notification.email.from:${spring.mail.username:}}")
    private String fromAddress;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public DeliveryResult send(DeliveryRequest request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromAddress != null && !fromAddress.isBlank()) {
                message.setFrom(fromAddress);
            }
            message.setTo(request.to());
            message.setSubject(request.subject() == null || request.subject().isBlank()
                    ? "Aionn notification"
                    : request.subject());
            message.setText(request.content());
            mailSender.send(message);
            return DeliveryResult.ok("smtp:" + request.notiId());
        } catch (MailException ex) {
            log.warn("Failed to send email notification {} to {}", request.notiId(), request.to(), ex);
            return DeliveryResult.failed("MAIL_SEND_FAILED", ex.getMessage());
        }
    }
}
