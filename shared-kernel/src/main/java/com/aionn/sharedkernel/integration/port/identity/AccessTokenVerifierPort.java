package com.aionn.sharedkernel.integration.port.identity;

import java.util.Optional;

public interface AccessTokenVerifierPort {

    Optional<String> verifyAndExtractUserId(String authorizationHeader);
}
