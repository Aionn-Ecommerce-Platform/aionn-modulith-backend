package com.aionn.identity.application.dto.consent.query;

import com.aionn.sharedkernel.application.query.Query;

public record GetMyConsentsQuery(String userId) implements Query {
}

