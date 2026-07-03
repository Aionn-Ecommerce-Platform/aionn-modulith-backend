package com.aionn.identity.application.usecase.user;

import com.aionn.identity.application.dto.user.command.RequestDataExportCommand;
import com.aionn.identity.application.dto.user.view.DataExportRequestView;
import com.aionn.identity.application.port.in.user.RequestDataExportInputPort;
import com.aionn.identity.application.service.AccountManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequestDataExportUseCase implements RequestDataExportInputPort {

    private final AccountManagementService accountManagementService;

    @Override
    @Transactional
    public DataExportRequestView execute(RequestDataExportCommand command) {
        return accountManagementService.requestDataExport(command);
    }
}

