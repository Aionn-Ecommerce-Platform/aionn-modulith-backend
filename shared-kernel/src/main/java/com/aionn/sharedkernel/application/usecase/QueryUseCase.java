package com.aionn.sharedkernel.application.usecase;

import com.aionn.sharedkernel.application.query.Query;

public interface QueryUseCase<Q extends Query, R> {
    R execute(Q query);
}
