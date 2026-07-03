package com.aionn.identity.application.port.out.user;

import com.aionn.identity.domain.model.IdentityUser;

import java.util.Optional;

public interface UserPersistencePort {

    IdentityUser save(IdentityUser user);

    Optional<IdentityUser> findById(String userId);

    Optional<IdentityUser> findByIdentity(String identity);

    boolean existsByPhone(String phone);

    boolean existsByUsername(String username);

    boolean existsById(String userId);
}
