package com.aionn.identity.application.dto.admin.command;

import com.aionn.sharedkernel.application.command.Command;
import com.aionn.identity.domain.valueobject.UserRole;

import java.util.Set;

public record UpdateUserRolesCommand(
                String userId,
                Set<UserRole> roles) implements Command {
}


