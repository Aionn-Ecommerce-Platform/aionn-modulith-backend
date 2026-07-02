package com.aionn.identity.adapter.rest.dto.security.response;

import java.util.List;

public record MfaResponse(
        boolean mfaEnabled,
        List<String> backupCodes
) {
}

