package com.aionn.identity.application.port.in.auth;

import com.aionn.identity.application.dto.auth.query.GetAuthSessionsQuery;
import com.aionn.identity.application.dto.auth.result.AuthSessionResult;

import java.util.List;

public interface GetAuthSessionsQueryPort {

    List<AuthSessionResult> execute(GetAuthSessionsQuery query);
}

