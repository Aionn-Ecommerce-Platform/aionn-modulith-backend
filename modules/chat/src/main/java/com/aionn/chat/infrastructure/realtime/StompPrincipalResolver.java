package com.aionn.chat.infrastructure.realtime;

import com.aionn.sharedkernel.integration.port.identity.AccessTokenVerifierPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompPrincipalResolver {

    private final AccessTokenVerifierPort accessTokenVerifier;

    public String resolveUserId(String authorizationHeader) {
        return accessTokenVerifier.verifyAndExtractUserId(authorizationHeader).orElse(null);
    }
}
