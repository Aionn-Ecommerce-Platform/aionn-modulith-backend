package com.aionn.identity.application.usecase.user;

import com.aionn.identity.application.port.in.user.ChangePhoneInputPort;
import com.aionn.identity.application.dto.user.view.UserProfileView;
import com.aionn.identity.application.service.AccountManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangePhoneUseCase implements ChangePhoneInputPort {

    private final AccountManagementService accountManagementService;

    @Override
    @Transactional
    public void sendOtp(String userId, String newPhone) {
        accountManagementService.requestPhoneChangeOtp(userId, newPhone);
    }

    @Override
    @Transactional
    public UserProfileView confirm(String userId, String otpCode) {
        return accountManagementService.confirmPhoneChange(userId, otpCode);
    }
}
