package com.aionn.identity.application.dto.auth.query;

import com.aionn.sharedkernel.application.query.Query;

public record GetAuthSessionsQuery(String userId) implements Query {
}

