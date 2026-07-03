package com.aionn.identity.application.port.in.admin;

import com.aionn.identity.application.dto.admin.command.RemoveUserRolesCommand;
import com.aionn.identity.application.dto.admin.result.UserRolesResult;

public interface RemoveUserRolesInputPort {
    UserRolesResult execute(RemoveUserRolesCommand command);
}
