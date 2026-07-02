package com.aionn.identity.application.port.in.user;

import com.aionn.identity.application.dto.user.command.RequestAccountDeletionCommand;
import com.aionn.identity.application.dto.user.view.DeletionRequestView;

public interface RequestAccountDeletionInputPort {

    DeletionRequestView execute(RequestAccountDeletionCommand command);
}
