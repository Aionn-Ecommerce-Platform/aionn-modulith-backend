package com.aionn.ordering.application.port.in.returns;

import com.aionn.ordering.application.dto.returns.result.ReturnResult;

public interface CancelReturnInputPort {

    ReturnResult execute(String returnId, String userId);
}
