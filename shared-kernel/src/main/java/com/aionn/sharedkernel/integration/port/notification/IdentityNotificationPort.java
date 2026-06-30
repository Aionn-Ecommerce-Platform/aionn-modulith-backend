package com.aionn.sharedkernel.integration.port.notification;

public interface IdentityNotificationPort {

    void sendPasswordResetRequested(String userId, String resetToken);

    void sendPasswordChanged(String userId, String channelHint);

    void sendEmailChanged(String userId, String oldEmail, String newEmail);

    void sendPhoneChanged(String userId, String oldPhone, String newPhone);

    void sendEmailOtp(String email, String otpCode);

    void sendPhoneOtp(String phoneNumber, String otpCode);

    void sendRegistrationOtp(String phoneNumber, String otpCode);
}
