package com.aionn.identity.application.dto.preference.command;

import com.aionn.sharedkernel.application.command.Command;

public record UpdateGeneralPreferenceCommand(
                String userId,
                String language,
                String currency,
                String timezone,
                String theme) implements Command {
}

