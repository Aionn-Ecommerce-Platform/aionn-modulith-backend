package com.aionn.identity.application.policy;

public final class UserValidationConstants {

    public static final int DISPLAY_NAME_MAX_LENGTH = 100;
    public static final int DISPLAY_NAME_MIN_LENGTH = 2;
    public static final int USERNAME_MAX_LENGTH = 50;
    public static final int USERNAME_MIN_LENGTH = 3;
    public static final int SOCIAL_USERNAME_SUFFIX_LENGTH = 10;
    public static final int AVATAR_URL_MAX_LENGTH = 2048;

    private UserValidationConstants() {
    }
}
