package com.aionn.identity.infrastructure.persistence.adapter.user;

import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.infrastructure.persistence.mapper.IdentityUserMapper;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserPersistencePort {

    private final UserRepository userRepository;
    private final IdentityUserMapper identityUserMapper;
    private final IdentityUserEntitySaveSupport identityUserEntitySaveSupport;

    @Override
    public IdentityUser save(IdentityUser user) {
        return identityUserEntitySaveSupport.save(user);
    }

    @Override
    public Optional<IdentityUser> findById(String userId) {
        return userRepository.findById(userId)
                .map(identityUserMapper::toDomain);
    }

    @Override
    public Optional<IdentityUser> findByIdentity(String identity) {
        return userRepository.findByEmailIgnoreCase(identity)
                .or(() -> userRepository.findByPhone(identity))
                .or(() -> userRepository.findByUsernameIgnoreCase(identity))
                .map(identityUserMapper::toDomain);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsernameIgnoreCase(username);
    }

    @Override
    public boolean existsById(String userId) {
        return userRepository.existsById(userId);
    }
}
