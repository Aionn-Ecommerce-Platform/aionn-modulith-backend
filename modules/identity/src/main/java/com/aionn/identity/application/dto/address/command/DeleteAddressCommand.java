package com.aionn.identity.application.dto.address.command;

import com.aionn.sharedkernel.application.command.Command;

public record DeleteAddressCommand(String userId, String addressId) implements Command {
}


