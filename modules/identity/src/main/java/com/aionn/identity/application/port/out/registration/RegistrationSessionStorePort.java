package com.aionn.identity.application.port.out.registration;

import com.aionn.identity.domain.model.RegistrationVerificationSession;

import java.util.Optional;

public interface RegistrationSessionStorePort {

    void save(RegistrationVerificationSession session);

    Optional<RegistrationVerificationSession> findByRegId(String regId);

    void deleteByRegId(String regId);
}
