package com.aionn.identity.application.port.in.address;

import com.aionn.identity.application.dto.address.command.DeleteAddressCommand;

public interface DeleteAddressInputPort {
    void execute(DeleteAddressCommand command);
}


