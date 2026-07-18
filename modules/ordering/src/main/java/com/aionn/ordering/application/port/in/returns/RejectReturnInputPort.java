package com.aionn.ordering.application.port.in.returns;

import com.aionn.ordering.application.dto.returns.command.RejectReturnCommand;
import com.aionn.ordering.application.dto.returns.result.ReturnResult;

public interface RejectReturnInputPort {
    ReturnResult execute(RejectReturnCommand command);
}