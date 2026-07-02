package com.aionn.identity.application.dto.admin.command;

import com.aionn.identity.domain.valueobject.UserStatus;

public record UpdateUserStatusCommand(
                String userId,
                UserStatus status) {
}


