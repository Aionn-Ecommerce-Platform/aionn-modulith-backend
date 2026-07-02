package com.aionn.identity.application.dto.address.command;

import com.aionn.sharedkernel.application.command.Command;

public record SetDefaultAddressCommand(String userId, String addressId) implements Command {
}


