package com.aionn.ordering.application.port.in.returns;

import com.aionn.ordering.application.dto.returns.command.ApproveReturnCommand;
import com.aionn.ordering.application.dto.returns.result.ReturnResult;

public interface ApproveReturnInputPort {
    ReturnResult execute(ApproveReturnCommand command);
}