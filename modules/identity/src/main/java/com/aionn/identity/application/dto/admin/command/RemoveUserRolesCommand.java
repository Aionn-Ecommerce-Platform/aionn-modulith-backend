package com.aionn.identity.application.dto.admin.command;

import com.aionn.identity.domain.valueobject.UserRole;

import java.util.Set;

public record RemoveUserRolesCommand(
        String userId,
        Set<UserRole> roles) {
}
