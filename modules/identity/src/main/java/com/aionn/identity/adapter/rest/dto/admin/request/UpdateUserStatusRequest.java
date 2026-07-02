package com.aionn.identity.adapter.rest.dto.admin.request;

import com.aionn.identity.domain.valueobject.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
                @NotNull(message = "Status is required") UserStatus status) {
}


