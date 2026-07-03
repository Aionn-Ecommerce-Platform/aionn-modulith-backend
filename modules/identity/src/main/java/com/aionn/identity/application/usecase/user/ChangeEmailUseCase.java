package com.aionn.identity.application.usecase.user;

import com.aionn.identity.application.dto.user.command.ConfirmEmailChangeCommand;
import com.aionn.identity.application.dto.user.command.RequestEmailChangeOtpCommand;
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
    public void requestOtp(RequestEmailChangeOtpCommand command) {
        accountManagementService.requestEmailChangeOtp(command.userId(), command.newEmail());
    }

    @Override
    @Transactional
    public UserProfileView confirm(ConfirmEmailChangeCommand command) {
        return accountManagementService.confirmEmailChange(command.userId(), command.otpCode());
    }
}
