package com.aionn.identity.application.usecase.user;

import com.aionn.identity.application.dto.user.command.RequestAccountDeletionCommand;
import com.aionn.identity.application.dto.user.view.DeletionRequestView;
import com.aionn.identity.application.port.in.user.RequestAccountDeletionInputPort;
import com.aionn.identity.application.service.AccountManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequestAccountDeletionUseCase implements RequestAccountDeletionInputPort {

    private final AccountManagementService accountManagementService;

    @Override
    @Transactional
    public DeletionRequestView execute(RequestAccountDeletionCommand command) {
        return accountManagementService.requestAccountDeletion(command);
    }
}

