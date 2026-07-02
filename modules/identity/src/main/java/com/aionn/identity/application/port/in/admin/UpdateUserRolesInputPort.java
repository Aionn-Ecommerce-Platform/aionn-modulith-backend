package com.aionn.identity.application.port.in.admin;

import com.aionn.identity.application.dto.admin.command.UpdateUserRolesCommand;
import com.aionn.identity.application.dto.admin.result.UserRolesResult;

public interface UpdateUserRolesInputPort {
    UserRolesResult execute(UpdateUserRolesCommand command);
}
