package com.aionn.identity.infrastructure.integration.user;

import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import com.aionn.sharedkernel.integration.port.identity.UserProfileLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class IdentityUserProfileLookupAdapter implements UserProfileLookupPort {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, UserProfile> findProfiles(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        u -> u.getUserId(),
                        u -> new UserProfile(
                                u.getUserId(),
                                u.getUsername(),
                                u.getDisplayName(),
                                u.getAvatarUrl()
                        ),
                        (existing, replacement) -> existing
                ));
    }
}
