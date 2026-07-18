package com.aionn.ordering.application.port.in.returns;

import com.aionn.ordering.application.dto.returns.command.ConfirmItemReceivedCommand;
import com.aionn.ordering.application.dto.returns.result.ReturnResult;

public interface ConfirmItemReceivedInputPort {
    ReturnResult execute(ConfirmItemReceivedCommand command);
}
