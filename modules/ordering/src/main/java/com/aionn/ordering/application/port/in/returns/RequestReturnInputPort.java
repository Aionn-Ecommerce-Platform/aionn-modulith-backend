package com.aionn.ordering.application.port.in.returns;

import com.aionn.ordering.application.dto.returns.command.RequestReturnCommand;
import com.aionn.ordering.application.dto.returns.result.ReturnResult;

public interface RequestReturnInputPort {
    ReturnResult execute(RequestReturnCommand command);
}
