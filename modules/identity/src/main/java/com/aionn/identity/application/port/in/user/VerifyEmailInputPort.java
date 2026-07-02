package com.aionn.identity.application.port.in.user;

public interface VerifyEmailInputPort {

    void sendOtp(String userId);

    void confirm(String userId, String otpCode);
}
