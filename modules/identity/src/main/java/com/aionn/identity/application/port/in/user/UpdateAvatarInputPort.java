package com.aionn.identity.application.port.in.user;

import com.aionn.identity.application.dto.user.command.UpdateAvatarCommand;
import com.aionn.identity.application.dto.user.view.UserProfileView;

public interface UpdateAvatarInputPort {

    UserProfileView execute(UpdateAvatarCommand command);
}



