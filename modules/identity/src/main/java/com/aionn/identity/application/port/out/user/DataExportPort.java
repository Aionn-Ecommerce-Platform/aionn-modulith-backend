package com.aionn.identity.application.port.out.user;

import com.aionn.identity.application.dto.user.view.DataExportRequestView;

public interface DataExportPort {

    DataExportRequestView save(String userId);

    boolean hasActiveRequest(String userId);
}
