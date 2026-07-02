package com.aionn.identity.application.port.in.user;

import com.aionn.identity.application.dto.user.command.UpdateDisplayNameCommand;
import com.aionn.identity.application.dto.user.view.UserProfileView;

public interface UpdateDisplayNameInputPort {

    UserProfileView execute(UpdateDisplayNameCommand command);
}




