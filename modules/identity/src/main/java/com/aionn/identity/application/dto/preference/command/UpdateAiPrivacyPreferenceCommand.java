package com.aionn.identity.application.dto.preference.command;

import com.aionn.sharedkernel.application.command.Command;

public record UpdateAiPrivacyPreferenceCommand(
                String userId,
                String aiPrivacySettingsJson) implements Command {
}



