package com.aionn.contracts.port.identity;

import java.util.Optional;

public interface AccessTokenVerifierPort {

    Optional<String> verifyAndExtractUserId(String authorizationHeader);
}
