package com.aionn.identity.application.usecase.user;

import com.aionn.identity.application.port.in.user.ChangeEmailInputPort;
import com.aionn.identity.application.dto.user.view.UserProfileView;
import com.aionn.identity.application.service.AccountManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangeEmailUseCase implements ChangeEmailInputPort {

    private final AccountManagementService accountManagementService;

    @Override
    @Transactional
    public void sendOtp(String userId, String newEmail) {
        accountManagementService.requestEmailChangeOtp(userId, newEmail);
    }

    @Override
    @Transactional
    public UserProfileView confirm(String userId, String otpCode) {
        return accountManagementService.confirmEmailChange(userId, otpCode);
    }
}
