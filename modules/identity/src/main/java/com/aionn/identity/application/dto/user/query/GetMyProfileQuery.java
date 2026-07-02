package com.aionn.identity.application.dto.user.query;

import com.aionn.sharedkernel.application.query.Query;

public record GetMyProfileQuery(String userId) implements Query {
}

