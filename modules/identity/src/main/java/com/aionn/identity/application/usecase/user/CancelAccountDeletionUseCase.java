package com.aionn.identity.application.usecase.user;

import com.aionn.identity.application.dto.user.command.CancelAccountDeletionCommand;
import com.aionn.identity.application.port.in.user.CancelAccountDeletionInputPort;
import com.aionn.identity.application.service.AccountManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelAccountDeletionUseCase implements CancelAccountDeletionInputPort {

    private final AccountManagementService accountManagementService;

    @Override
    @Transactional
    public void execute(CancelAccountDeletionCommand command) {
        accountManagementService.cancelAccountDeletion(command);
    }
}

