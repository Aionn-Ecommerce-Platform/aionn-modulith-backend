package com.aionn.identity.application.port.in.address;

import com.aionn.identity.application.dto.address.result.AddressResult;
import com.aionn.identity.application.dto.address.command.UpdateAddressCommand;

public interface UpdateAddressInputPort {
    AddressResult execute(UpdateAddressCommand command);
}


