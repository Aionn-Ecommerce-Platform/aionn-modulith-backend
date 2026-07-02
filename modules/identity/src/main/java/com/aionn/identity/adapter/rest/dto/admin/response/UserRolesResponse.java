package com.aionn.identity.adapter.rest.dto.admin.response;

import com.aionn.identity.domain.valueobject.UserRole;

import java.util.Set;

public record UserRolesResponse(
                Set<UserRole> roles) {
}


