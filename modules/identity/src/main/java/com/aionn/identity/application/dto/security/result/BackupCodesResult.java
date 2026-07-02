package com.aionn.identity.application.dto.security.result;

import java.util.List;

public record BackupCodesResult(
                List<String> backupCodes) {
}

