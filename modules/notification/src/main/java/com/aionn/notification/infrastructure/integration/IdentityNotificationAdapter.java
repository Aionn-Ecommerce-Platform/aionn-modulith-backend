package com.aionn.notification.infrastructure.integration;

import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.service.NotificationDeliveryOrchestrator;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.contracts.port.notification.IdentityNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Bridges identity use cases to notifications.
 *
 * <p>OTP and security mails are persisted first and delivered on {@code notificationTaskExecutor}, so a
 * slow SMTP server adds latency to the mail, not to the login or registration request that triggered it.
 * The rows stay {@code PENDING} if the async trigger is lost, which the retry sweep picks up.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityNotificationAdapter implements IdentityNotificationPort {

    private static final String EVENT_PASSWORD_CHANGED = "identity.password-changed";
    private static final String EVENT_EMAIL_CHANGED = "identity.email-changed";
    private static final String EVENT_PHONE_CHANGED = "identity.phone-changed";
    private static final String EVENT_EMAIL_OTP = "identity.email-otp";
    private static final String EVENT_PHONE_OTP = "identity.phone-otp";
    private static final String EVENT_REGISTRATION_OTP = "identity.registration-otp";

    private final NotificationDeliveryOrchestrator deliveryOrchestrator;

    @Override
    public void sendPasswordChanged(String userId, String channelHint) {
        enqueueByEvent(new NotificationCommands.SendByEvent(
                userId, EVENT_PASSWORD_CHANGED, NotificationCategory.SECURITY,
                null, null, null,
                Map.of("channelHint", channelHint == null ? "" : channelHint)));
    }

    @Override
    public void sendEmailChanged(String userId, String oldEmail, String newEmail) {
        enqueueByEvent(new NotificationCommands.SendByEvent(
                userId, EVENT_EMAIL_CHANGED, NotificationCategory.SECURITY,
                List.of(NotificationChannel.EMAIL), null, null,
                Map.of("oldEmail", nullToEmpty(oldEmail), "newEmail", nullToEmpty(newEmail))));
    }

    @Override
    public void sendPhoneChanged(String userId, String oldPhone, String newPhone) {
        enqueueByEvent(new NotificationCommands.SendByEvent(
                userId, EVENT_PHONE_CHANGED, NotificationCategory.SECURITY,
                List.of(NotificationChannel.SMS), null, null,
                Map.of("oldPhone", nullToEmpty(oldPhone), "newPhone", nullToEmpty(newPhone))));
    }

    @Override
    public void sendEmailOtp(String email, String otpCode) {
        enqueueDirect(new NotificationCommands.SendDirectByEvent(
                email, EVENT_EMAIL_OTP, NotificationCategory.SECURITY,
                NotificationChannel.EMAIL, email, null, null,
                Map.of("otpCode", otpCode)));
    }

    @Override
    public void sendPhoneOtp(String phoneNumber, String otpCode) {
        enqueueDirect(new NotificationCommands.SendDirectByEvent(
                phoneNumber, EVENT_PHONE_OTP, NotificationCategory.SECURITY,
                NotificationChannel.SMS, phoneNumber, null, null,
                Map.of("otpCode", otpCode)));
    }

    @Override
    public void sendRegistrationOtp(String phoneNumber, String otpCode) {
        enqueueDirect(new NotificationCommands.SendDirectByEvent(
                phoneNumber, EVENT_REGISTRATION_OTP, NotificationCategory.SECURITY,
                NotificationChannel.SMS, phoneNumber, null, null,
                Map.of("otpCode", otpCode)));
    }

    private void enqueueByEvent(NotificationCommands.SendByEvent command) {
        safeDispatch(() -> deliveryOrchestrator.deliverQueuedAsync(
                deliveryOrchestrator.enqueueByEvent(command)));
    }

    private void enqueueDirect(NotificationCommands.SendDirectByEvent command) {
        safeDispatch(() -> deliveryOrchestrator.deliverQueuedAsync(
                List.of(deliveryOrchestrator.enqueueDirectByEvent(command))));
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private void safeDispatch(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            log.warn("Identity notification dispatch failed: {}", ex.getMessage());
        }
    }
}
