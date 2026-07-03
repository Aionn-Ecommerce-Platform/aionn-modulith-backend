package com.aionn.identity.application.port.in.admin;

import com.aionn.identity.application.dto.admin.command.UpdateUserStatusCommand;
import com.aionn.identity.application.dto.admin.result.UserStatusResult;

public interface UpdateUserStatusInputPort {
    UserStatusResult execute(UpdateUserStatusCommand command);
}
