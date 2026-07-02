package com.aionn.identity.application.dto.user.view;

public record UserActionOutcomeView(
        String action,
        String message,
        UserProfileView profile) {
}
