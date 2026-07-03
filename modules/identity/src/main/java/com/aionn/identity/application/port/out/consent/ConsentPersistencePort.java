package com.aionn.identity.application.port.out.consent;

import com.aionn.identity.application.dto.consent.result.ConsentResult;
import com.aionn.identity.domain.model.UserConsent;

import java.util.List;
import java.util.Optional;

public interface ConsentPersistencePort {

    ConsentResult append(UserConsent consent);

    Optional<ConsentResult> findLatest(String userId, String consentType);

    List<ConsentResult> findHistory(String userId);
}
