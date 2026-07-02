package com.aionn.identity.application.dto.security.result;

import java.util.List;

public record MfaResult(
                boolean mfaEnabled,
                List<String> backupCodes) {
}
