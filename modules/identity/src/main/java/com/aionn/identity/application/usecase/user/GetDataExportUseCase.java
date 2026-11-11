package com.aionn.identity.application.usecase.user;

import com.aionn.identity.application.dto.user.view.DataExportPayloadView;
import com.aionn.identity.application.port.in.user.GetDataExportInputPort;
import com.aionn.identity.application.service.AccountManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetDataExportUseCase implements GetDataExportInputPort {

    private final AccountManagementService accountManagementService;

    @Override
    @Transactional(readOnly = true)
    public DataExportPayloadView execute(String userId, String requestId) {
        return accountManagementService.getDataExport(userId, requestId);
    }
}
