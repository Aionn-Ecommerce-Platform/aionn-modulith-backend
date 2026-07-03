package com.aionn.identity.application.port.out.social;

import com.aionn.identity.domain.valueobject.AuthProvider;

public interface SocialTokenVerifierPort {
	SocialUserProfile verifyAndExtract(AuthProvider provider, String providerToken);
}
