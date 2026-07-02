package com.aionn.identity.application.port.in.user;

import com.aionn.identity.application.dto.user.command.CancelAccountDeletionCommand;

public interface CancelAccountDeletionInputPort {

    void execute(CancelAccountDeletionCommand command);
}



