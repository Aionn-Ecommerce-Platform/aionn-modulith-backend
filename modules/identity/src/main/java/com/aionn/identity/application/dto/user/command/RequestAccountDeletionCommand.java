package com.aionn.identity.application.dto.user.command;

import com.aionn.sharedkernel.application.command.Command;

public record RequestAccountDeletionCommand(String userId, String password, String mfaCode) implements Command {

    public RequestAccountDeletionCommand(String userId) {
        this(userId, null, null);
    }
}
