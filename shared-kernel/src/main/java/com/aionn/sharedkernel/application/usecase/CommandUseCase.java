package com.aionn.sharedkernel.application.usecase;

import com.aionn.sharedkernel.application.command.Command;

public interface CommandUseCase<C extends Command, R> {
    R execute(C command);
}
