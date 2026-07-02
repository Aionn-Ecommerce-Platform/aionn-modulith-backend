package com.aionn.identity.adapter.rest.dto.security.response;

import java.util.List;

public record BackupCodesResponse(
        List<String> backupCodes
) {
}


