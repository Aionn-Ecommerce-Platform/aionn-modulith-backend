package com.aionn.identity.application.port.in.user;

import com.aionn.identity.application.dto.user.command.RequestDataExportCommand;
import com.aionn.identity.application.dto.user.view.DataExportRequestView;

public interface RequestDataExportInputPort {

    DataExportRequestView execute(RequestDataExportCommand command);
}



