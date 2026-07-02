package com.aionn.identity.application.usecase.user;

import com.aionn.identity.application.port.in.user.VerifyEmailInputPort;
import com.aionn.identity.application.service.AccountManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VerifyEmailUseCase implements VerifyEmailInputPort {

    private final AccountManagementService accountManagementService;

    @Override
    @Transactional
    public void sendOtp(String userId) {
        accountManagementService.sendVerifyPrimaryEmailOtp(userId);
    }

    @Override
    @Transactional
    public void confirm(String userId, String otpCode) {
        accountManagementService.confirmVerifyPrimaryEmailOtp(userId, otpCode);
    }
}
