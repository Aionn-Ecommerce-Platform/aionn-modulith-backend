package com.aionn.identity.application.policy;

public final class MfaConstants {

    public static final int TOTP_CODE_LENGTH = 6;
    public static final int BACKUP_CODE_LENGTH = 8;
    public static final int MFA_ENCRYPTION_KEY_MIN_LENGTH = 32;

    private MfaConstants() {
    }
}
