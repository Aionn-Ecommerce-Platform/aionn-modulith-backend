package com.aionn.identity.application.dto.registration.command;

import com.aionn.sharedkernel.application.command.Command;

public record ResendRegistrationOtpCommand(
		String regId,
		String ipAddress) implements Command {
}



