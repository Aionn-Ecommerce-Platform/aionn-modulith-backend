package com.aionn.identity.application.dto.preference.command;

public record UpdateAiPrivacyPreferenceCommand(
                String userId,
                String aiPrivacySettingsJson) {
}



