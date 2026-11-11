package com.aionn.identity.application.port.in.user;

import com.aionn.identity.application.dto.user.view.DataExportPayloadView;

public interface GetDataExportInputPort {

    DataExportPayloadView execute(String userId, String requestId);
}
