package com.aionn.identity.application.usecase.auth;

import com.aionn.identity.application.dto.auth.command.LinkSocialCommand;
import com.aionn.identity.application.dto.auth.result.SocialLinkResult;
import com.aionn.identity.application.mapper.AuthResultMapper;
import com.aionn.identity.application.port.in.auth.LinkSocialInputPort;
import com.aionn.identity.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LinkSocialUseCase implements LinkSocialInputPort {

    private final AuthService authService;
    private final AuthResultMapper authResultMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SocialLinkResult execute(LinkSocialCommand command) {
        var socialLink = authService.linkSocial(command);
        return authResultMapper.toSocialLinkResult(socialLink);
    }
}
