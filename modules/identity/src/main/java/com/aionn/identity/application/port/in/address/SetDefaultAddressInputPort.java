package com.aionn.identity.application.port.in.address;

import com.aionn.identity.application.dto.address.result.AddressResult;
import com.aionn.identity.application.dto.address.command.SetDefaultAddressCommand;

public interface SetDefaultAddressInputPort {
    AddressResult execute(SetDefaultAddressCommand command);
}


