package com.aionn.identity.application.port.in.auth;

import com.aionn.identity.application.dto.auth.command.LinkSocialCommand;
import com.aionn.identity.application.dto.auth.result.SocialLinkResult;

public interface LinkSocialInputPort {

    SocialLinkResult execute(LinkSocialCommand command);
}

