package com.aionn.identity.application.dto.admin.query;

import com.aionn.sharedkernel.application.query.Query;

public record GetUserQuery(String userId) implements Query {
}

