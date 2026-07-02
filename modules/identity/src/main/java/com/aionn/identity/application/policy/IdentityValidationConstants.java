package com.aionn.identity.application.policy;

public final class IdentityValidationConstants {

    public static final int DISPLAY_NAME_MAX_LENGTH = 100;
    public static final int DISPLAY_NAME_MIN_LENGTH = 2;
    public static final int AVATAR_URL_MAX_LENGTH = 2048;

    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 128;

    public static final int RESET_TOKEN_BYTES = 32;
    public static final int REFRESH_TOKEN_BYTES = 48;
    public static final int AGENT_KEY_BYTES = 32;

    private IdentityValidationConstants() {
    }
}
