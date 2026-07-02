package com.aionn.identity.application.port.in.auth;

import com.aionn.identity.application.dto.auth.command.UnlinkSocialCommand;

public interface UnlinkSocialInputPort {

    void execute(UnlinkSocialCommand command);
}

